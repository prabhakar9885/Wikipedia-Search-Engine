import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import SharedLibs.PageInfo;
import SharedLibs.Stemmer;
import SharedLibs.UtilFuncs;

/***
 * 
 * @author prabhakar The Application is meant for processing the Query and then
 *         performing the Search via., index files.
 *
 */
public class Main {

	static private HashMap<String, Long> primaryIndex = new HashMap<String, Long>();
	static private HashMap<String, Long> secondaryIndex = new HashMap<String, Long>();
	static HashSet<String> queryTermsStemmed = new HashSet<String>();
	static HashMap<String, String> fieldQueryTerms = new HashMap<String, String>();
	static ArrayList<ArrayList<PageInfo>> tempPostingList = new ArrayList<ArrayList<PageInfo>>();
	static RandomAccessFile randomAccessFile;
	static SeekableByteChannel sbc;
	static StringBuilder sbTemp = new StringBuilder();
	static ByteBuffer bf = ByteBuffer.allocate(1024);
	static String pathToPostings;
	static String encoding = System.getProperty("file.encoding");

	public static void main(String[] args) throws IOException {

		Scanner scn = new Scanner(System.in);
		String[] queryTerms;
		Stemmer stem = new Stemmer();
		String pathToPrimaryIndex = args[0] + (args[0].endsWith("/") ? "index.PrimaryIndex" : "/index.PrimaryIndex");
		String pathToSecondaryIndex = args[0]
				+ (args[0].endsWith("/") ? "index.SecondaryIndex" : "/index.SecondaryIndex");
		pathToPostings = args[0] + (args[0].endsWith("/") ? "postings" : "/postings");

		loadIndexIntoHashMap(pathToPrimaryIndex, pathToSecondaryIndex);
		// randomAccessFile = new RandomAccessFile(pathToPostings, "r");

		while (true) {
			System.out.print("Query String: ");
			String query = scn.nextLine().toLowerCase();
			long lStartTime = System.currentTimeMillis();

			if (query.equals("exit"))
				break;

			// Convert Query into HashSet<Stemmed String>
			queryTerms = query.split(" ");
			queryTermsStemmed.clear();
			fieldQueryTerms.clear();

			for (String string : queryTerms) {
				int indexOfDelim = string.indexOf(':');
				if (indexOfDelim == -1) {
					// The query term is a normal word
					stem.add(string);
					stem.stem();
				} else {
					// The query term is a field query
					String[] str = string.split(":");
					if (fieldQueryTerms.containsKey(str[1]))
						str[0] = str[0] + fieldQueryTerms.get(str[1]);
					stem.add(str[1]);
					stem.stem();
					fieldQueryTerms.put(stem.toString(), str[0]);
				}
				string = stem.toString();
				queryTermsStemmed.add(string);
			}

			searchForQuery(pathToPostings);
			System.out.println("Service time: " + (System.currentTimeMillis() - lStartTime) + " ms");
		}
		scn.close();
		// randomAccessFile.close();
		sbc.close();
	}

	/***
	 * Auxiliary method for loading Primary and Secondary files into in-memory
	 * data-structures.
	 * 
	 * @param pathToPrimaryIndex
	 * @param pathToSecondaryIndex
	 * @throws IOException
	 */
	private static void loadIndexIntoHashMap(String pathToPrimaryIndex, String pathToSecondaryIndex)
			throws IOException {
		BufferedReader bufferedReader = null;
		String line;
		StringBuffer sbKey = new StringBuffer();

		try {
			bufferedReader = new BufferedReader(new FileReader(pathToPrimaryIndex));
			while ((line = bufferedReader.readLine()) != null) {
				int i = 0;
				sbKey.setLength(0);
				while (line.charAt(i) != ':')
					sbKey.append(line.charAt(i++));
				primaryIndex.put(sbKey.toString(), Long.parseLong(line.substring(i + 1)));
			}
		} catch (FileNotFoundException e) {
			System.out.println("Primary Index not Found: " + pathToPrimaryIndex);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}

		try {
			bufferedReader = new BufferedReader(new FileReader(pathToSecondaryIndex));
			while ((line = bufferedReader.readLine()) != null) {
				int i = 0;
				sbKey.setLength(0);
				while (line.charAt(i) != ':')
					sbKey.append(line.charAt(i++));
				secondaryIndex.put(sbKey.toString(), Long.parseLong(line.substring(i + 1)));
			}
		} catch (FileNotFoundException e) {
			System.out.println("Secondary Index not Found: " + pathToSecondaryIndex);
			e.printStackTrace();
		} finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}
	}

	/***
	 * Performs the QuerySearch in the PostingsFile
	 * 
	 * @param pathToPostings
	 * @throws IOException
	 */
	private static void searchForQuery(String pathToPostings) throws IOException {

		Map<String, String> termPostsMapping = new LinkedHashMap<String, String>();

		for (String term : queryTermsStemmed) {
			if (!primaryIndex.containsKey(term))
				continue;
			long bytePositionInPostingsFile = primaryIndex.get(term);
			String posts = getPostingListFromPrimIndexFile(bytePositionInPostingsFile);
			termPostsMapping.put(term, posts.substring(posts.indexOf(":") + 1));
		}

		ArrayList<PageInfo> intersection = getIntersectionFor(termPostsMapping);
		if (intersection == null)
			return;

		int i = 0;
		for (PageInfo page : intersection) {
			if (i == 10)
				break;
			System.out.print(page.docId + "-" + page.rank + " : ");
			i++;
		}
		System.out.println();

		tempPostingList.clear();
	}

	private static String getPostingListFromPrimIndexFile(long bytePositionInPostingsFile) throws IOException {
		// randomAccessFile.seek(bytePositionInPostingsFile);
		sbc = Files.newByteChannel(Paths.get(pathToPostings), StandardOpenOption.READ);

		sbc.position(bytePositionInPostingsFile);
		sbTemp.setLength(0);
		bf.clear();
		String str = null;

		while (sbc.read(bf) > 0) {
			bf.flip();
			str = Charset.forName(encoding).decode(bf).toString();
			if (str.contains("\n")) {
				sbTemp.append(str.substring(0, str.indexOf('\n')));
				break;
			} else
				sbTemp.append(str);
			bf.rewind();
		}
		sbc.close();
		return sbTemp.toString();
	}

	/***
	 * Returns the posting-list pertaining to the given term
	 * 
	 * @param termPostsMapping
	 */
	private static ArrayList<PageInfo> getIntersectionFor(Map<String, String> termPostsMapping) {

		if (termPostsMapping.size() == 0)
			return null;

		String[] keys = new String[termPostsMapping.size()];
		termPostsMapping.keySet().toArray(keys);

		// Convert termsPostingMapping into tempPostingList(which holds PageInfo
		// Obj)
		for (Map.Entry<String, String> map : termPostsMapping.entrySet()) {
			ArrayList<PageInfo> list1 = UtilFuncs.getPostingListAsPageInfoList(termPostsMapping.get(map.getKey()),
					fieldQueryTerms.get(map.getKey()));
			tempPostingList.add(list1);
		}

		// Sort tempPostingList based on the number of objects present at each
		// index.
		tempPostingList.sort((p1, p2) -> p1.size() > p2.size() ? 0 : -1);

		ArrayList<PageInfo> list1 = tempPostingList.get(0);

		// Perform Intersection of all the posting lists
		for (int i = 1; i < tempPostingList.size(); i++) {
			ArrayList<PageInfo> ans = new ArrayList<PageInfo>();
			ArrayList<PageInfo> list2 = tempPostingList.get(i);
			int nList1 = list1.size(), nList2 = list2.size();
			int iList1 = 0, iList2 = 0;
			while (iList1 < nList1 && iList2 < nList2) {
				long interVal = list1.get(iList1).docId;
				long postVal = list2.get(iList2).docId;
				if (interVal == postVal) {
					ans.add(list1.get(iList1));
					iList1++;
					iList2++;
				} else if (interVal < postVal) {
					iList1++;
				} else {
					iList2++;
				}
			}
			list1 = ans;
		}

		list1.sort((p1, p2) -> p1.rank < p2.rank ? 0 : -1);

		return list1;
	}

}
