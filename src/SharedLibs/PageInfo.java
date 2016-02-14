package SharedLibs;

public class PageInfo {

	public int frequency, noOfTerms = 1;
	public int infoboxFrequeny = 0, noOfTermsInInfoBox = 0;
	public int titleFrequeny = 0, noOfTermsInTitle = 0;
	public int categoryFrequeny = 0, noOfTermsInCategory = 0;
	public int refFrequeny = 0;
	public int extLinkFrequeny = 0;
	public long docId;

	public int weightedSum = 0; // Equivalent for tf(t,d)
	public long rank = 0;

	public PageInfo() {
		frequency = 1;
	}

	public void computeWightedSum() {
		rank = (long) ((noOfTermsInTitle > 0 ? 10000 * titleFrequeny * 1.0 / noOfTermsInTitle : 0)
				+ 2000 * (noOfTermsInInfoBox > 0 ? infoboxFrequeny * 1.0 / noOfTermsInInfoBox : 0)
				+ 1000 * (noOfTermsInCategory > 0 ? categoryFrequeny * 1.0 / noOfTermsInCategory : 0)
				+ 100 * (frequency * 1.0 / noOfTerms));
	}

}