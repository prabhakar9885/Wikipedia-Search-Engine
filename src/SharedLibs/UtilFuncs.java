package SharedLibs;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilFuncs {

	private static Stemmer stemmer = new Stemmer();
	private static StopWords sw = new StopWords();


	/***
	 * Convert a Posting into PageInfo Object
	 * 
	 * @param posting
	 * @param fieldQueryTerms
	 * @return
	 */
	public static ArrayList<PageInfo> getPostingListAsPageInfoList(String posting, String fieldQueryTerms) {
		String[] list1 = posting.split("\\|");
		StringBuilder sbTemp = new StringBuilder();
		ArrayList<PageInfo> pageInfoList = new ArrayList<PageInfo>();

		if (fieldQueryTerms == null)
			fieldQueryTerms = "";

		for (int i = 0; i < list1.length; i++) {

			String postingStr = list1[i];
			int len = postingStr.length();

			if (list1[i].contains("10331"))
				i = i;

			int indexOfDelimitter = postingStr.indexOf('-', 0);
			PageInfo pi = new PageInfo();

			pi.docId = Long.parseLong(postingStr.substring(0, indexOfDelimitter));

			int j = indexOfDelimitter + 1;
			sbTemp.setLength(0);
			while (j < len && postingStr.charAt(j) != '-' && postingStr.charAt(j) != '|')
				sbTemp.append(postingStr.charAt(j++));
			pi.rank = Long.parseLong(sbTemp.toString());
			pi.categoryFrequeny = pi.extLinkFrequeny = pi.infoboxFrequeny = pi.titleFrequeny = pi.refFrequeny = 0;

			// Not a field-query => add the PageInfo to the result-set
			if (fieldQueryTerms.length() == 0) {
				pageInfoList.add(pi);
				continue;
			}

			// Its a field-query but the meta-data(T/C/I/R/E) is not available
			// on PageInfo => DONT add the PageInfo to the result-set
			if (j == len) {
				continue;
			}

			if (postingStr.indexOf('C', j + 1) != -1) {
				if (fieldQueryTerms.contains("c")) {
					pi.categoryFrequeny = 1;
					pageInfoList.add(pi);
				}
			}
			if (postingStr.indexOf('I', j + 1) != -1) {
				if (fieldQueryTerms.contains("i")) {
					pi.infoboxFrequeny = 1;
					pageInfoList.add(pi);
					continue;
				}
			}
			if (postingStr.indexOf('T', j + 1) != -1) {
				if (fieldQueryTerms.contains("t")) {
					pi.titleFrequeny = 1;
					pageInfoList.add(pi);
					continue;
				}
			}
			if (postingStr.indexOf('R', j + 1) != -1) {
				if (fieldQueryTerms.contains("r")) {
					pi.refFrequeny = 1;
					pageInfoList.add(pi);
					continue;
				}
			}
			if (postingStr.indexOf('E', j + 1) != -1) {
				if (fieldQueryTerms.contains("e")) {
					pi.categoryFrequeny = 1;
					pageInfoList.add(pi);
					continue;
				}
			}

		}

		return pageInfoList;

	}

	public static ArrayList<String> getTokensAsList(String textStr, String delim) {
		ArrayList<String> tokensList = new ArrayList<String>();
		StringTokenizer strTok = new StringTokenizer(textStr, delim);
		String temp;

		while (strTok.hasMoreTokens()) {
			temp = strTok.nextToken();

			if (sw.isStopWord(temp))
				continue;

			stemmer.add(temp);
			stemmer.stem();
			temp = stemmer.toString();
			if (temp.length() > 0)
				tokensList.add(temp.trim());
		}
		return tokensList;
	}

	public static int indexOf(String patternString, String string, int startIndex) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(string.substring(startIndex));
		return matcher.find() ? matcher.start() : -1;
	}

	public static int indexOf(String patternString, String string) {
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(string);
		return matcher.find() ? matcher.start() : -1;
	}

}
