package contracts;

import java.io.IOException;
import java.util.regex.Pattern;

import xbrl.Utils;

public class PatternsDif {

	public static String[] stateAndCountryLongNames = { "ALASKA", "ALABAMA", "ARKANSAS", "AMERICAN[\r\n ]{0,3}SAMOA",
			"ARIZONA", "CALIFORNIA", "COLORADO", "CONNECTICUT", "DISTRICT[\r\n ]{0,3}OF[\r\n ]{0,3}COLUMBIA",
			"DELAWARE", "FLORIDA", "FEDERATED[\r\n ]{0,3}STATES[\r\n ]{0,3}OF[\r\n ]{0,3}MICRONESIA", "GEORGIA", "GUAM",
			"HAWAII", "IOWA", "IDAHO", "ILLINOIS", "INDIANA", "KANSAS", "KENTUCKY", "LOUISIANA", "MASSACHUSETTS",
			"MARYLAND", "MAINE", "MARSHALL[\r\n ]{0,3}ISLANDS", "MICHIGAN", "MINNESOTA", "MISSOURI",
			"NORTHERN[\r\n ]{0,3}MARIANA[\r\n ]{0,3}ISLANDS", "MISSISSIPPI", "MONTANA", "NORTH[\r\n ]{0,3}CAROLINA",
			"NORTH[\r\n ]{0,3}DAKOTA", "NEBRASKA", "NEW[\r\n ]{0,3}HAMPSHIRE", "NEW[\r\n ]{0,3}JERSEY",
			"NEW[\r\n ]{0,3}MEXICO", "NEVADA", "NEW[\r\n ]{0,3}YORK", "OHIO", "OKLAHOMA", "OREGON", "PENNSYLVANIA",
			"PUERTO[\r\n ]{0,3}RICO", "PALAU", "RHODE[\r\n ]{0,3}ISLAND", "SOUTH[\r\n ]{0,3}CAROLINA",
			"SOUTH[\r\n ]{0,3}DAKOTA", "TENNESSEE", "TRUST[\r\n ]{0,3}TERRITORIES", "TEXAS", "UTAH",
			"WEST[\r\n ]{0,3}VIRGINIA", "VIRGIN[\r\n ]{0,3}ISLANDS", "VERMONT", "WASHINGTON", "WISCONSIN", "VIRGINIA",
			"WYOMING" };

	// each of the above and below string[] arrays must line up so that I can
	// run loop.

	// replace with ZZ preceding b/c I can then use ZZ as common word in order
	// to treat each of these as if they were the same for aggregation of common
	// patterns.
	public static String[] stateAndCountryAbbrevs = { "AK", "AL", "AR", "AS", "AZ", "CA", "CO", "CT", "DC", "DE", "FL",
			"FM", "GA", "GU", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD", "ME", "MH", "MI", "MN", "MO",
			"MP", "MS", "MT", "NC", "ND", "NE", "NH", "NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "PR", "PW", "RI",
			"SC", "SD", "TN", "TT", "TX", "UT", "WV", "VI", "VT", "WA", "WI", "VA", "WY" };

	public static Pattern patternStopWordsOf2 = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(with respect)|(in respect)|(respective)|(set forth)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternStopWords = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + ""
			+ "(in case)|(a)|(about)|(above)|(after)|(again)|(all)|(am)|(an)|(any)|(and)|(are)|(as)|(at)|(be)"
			+ "|(because)|(been)|(before)|(being)|(below)|(between)|(both)|(by)|(can)|(did)|(do)|(does)|(doing)|(down)"
			+ "|(during)|(each)|(either)|(few)|(for)|(from)|(further)|(had)|(has)|(have)|(having)|(he)|(her)|(here)|(hers)"
			+ "|(herself)|(him)|(himself)|(his)|(how)|(I)|(if)|(in)|(into)|(is)|(it)|(its)|(it\'s)|(itself)|(just)|(me)"
			+ "|(might)|(more)|(most)|(must)|(my)|(myself)|(need)|(now)|(of)|(off)|(on)|(once)|(only)|(or)|(other)|(otherwise)"
			+ "|(our)|(ours)|(ourselves)|(out)|(over)|(own)|(same)|(she)|(she\'s)|(should)|(so)|(some)|(such)|(than)"
			+ "|(that)|(the)|(their)|(theirs)|(them)|(themselves)|(then)|(there)|(these)|(they)|(this)|(those)|(through)"
			+ "|(to)|(too)|(under)|(until)|(up)|(very)|(very)|(was)|(we)|(were)|(what)|(when)|(where)|(whether)|(which)|(while)|(who)"
			+ "|(whom)|(why)|(will)|(with)|(won)|(would)|(you)|(your)|(yours)|(yourself)|(yourselves)" + ""
			+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternStopWordsContract = Pattern.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))"
			+ "(bonds?|notes?|debenture?s?|deed" + "|(certificate|note)holders?|certificates?|holders?"
			+ "|senior|junior|convertible|preferred|(this|the)[\r\n ]{1,3}(debt)"
			+ "|class|series|[A-Z]-\\d|securit[iesy]{1,3}"
			+ "|(this|the)[\r\n ]{1,3}(first|second|third|fourth|fifth|sixth)"
			+ "|supplementa?l?|indenture?|pooling and servicing|agreement" + "|(this|the)[\r\n ]{1,3}"
			+ "(trust)|guarante[yees]{1,3}|collateral documents?|documents?"
			+ "|[12]{1}[09]{1}[\\d]{1}-?\\d?)([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	public static Pattern patternStopWordsLegal = Pattern
			.compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(aforementioned)|(aforesaid)|(applicable)|(foregoing)"
					+ "|(hereafter)|(hereby)|(herein)|(hereof)|(hereunder)|(herewith)|(hereto)|(means)"
					+ "|(pursuant)|(relation)|(relating)|(related)"
					+ "|(thereafter)|(thereby)|(therefor)|(therefore)|(therein)|(thereof)|(thereto)|(thereunder)"
					+ "|(regarding)|(respect)" + ")([\r\n\t \\)\\]\\}\":;\\.,]{1}|$)");

	// cannot be case insensitive.
	public static Pattern patternStopWordsLegalEntitySpecific = Pattern
			.compile("(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" + "(Administrators?)|(Affiliates?)|(Company)|((Conversion"
					+ "|Collateral|(Issuing and )?Paying )Agents?)|(Guarantor)" + "|(Issuer)|(Obligor)"
					+ "|((Security )?(Registrar))|(Trustee)" + "|(Agents?)|((Domestic ?)(Restricted ?)Subsidiary)"
					+ ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	// ==>this pattern is faulty -- replaces everything. Need to also ignore
	// first word given it is start of sentence - so this may be too complex.
	// But if I go specific route above the variety is endless.

	// public static Pattern patternStopWordsLegalEntity = Pattern
	// .compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" +
	// "([A-Z]{1}[a-z]{1,15})" +
	// ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");

	/*
	 * public static Pattern patternStopWordsLegal = Pattern
	 * .compile("(?i)(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" +
	 * "(aforementioned)|(applicable)|(hereof)|(hereunder)|(thereof)|(thereunder)" +
	 * ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
	 * 
	 * // remove any initial cap word - this will corrupt where input or output is
	 * // all caps. Where all caps I'll need to address using specific entity //
	 * names.
	 * 
	 * public static Pattern patternStopWordsLegalEntity = Pattern
	 * .compile("(?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" +
	 * "([A-Z]{1}[a-z]{1,20})([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
	 * 
	 * public static Pattern patternStopWordsLegalEntitySpecific = Pattern
	 * .compile("((?i)?<=([\r\n\t \\(\\[\\{\"]{1}|^))(" +
	 * "(Administrator)|(Agent)|(Company)|(Collateral Agent)|(Issuer)|(Trustee)|(Subsidiary)"
	 * + ")([\r\n\t \\)\\]\\}\":;\\.]{1}|$)");
	 */

//	public static Pattern patternClosingPara = Pattern
//			.compile("((?sm)((?<!A((?i)uthenticat).{1,350}))(IN|In) ? ?(WITNESS.{1,3}WHEREOF)"
//					+ ".{1,150}((?ism)(executed|signed|set.{1,4}their.{1,4}hand).{1,250}\\.))");
	// TODO: THIS PATTERN IS VERY SLOW! B/C OF THE 350 AND 250 REQUIREMENTS. I
	// CAN ELIMINATE THE 350 BY SEEING TEXT.LEN AND IF THIS IS ONLY 70% THRU AND
	// ANOTHER WITNESS WHEREOF OCCURS.

	// find first exhibit in contract. Used as ruff tool to cut body of
	// contract.

	public static Pattern patternExhibitHeader = Pattern.compile("(?sm)([\r\n]{3,} ?|[\t]{5,}|[ ]{8,})"
			+ "(?=(ANNEX|EXHIBIT|SCHEDULE|APPENDIX|Annex|Exhibit|Schedule|Appendix)(?!s|S)[A-Z\\d- ]{0,7}[ \t]{0,5}[\r\n]{2})"
			+ "(?!(.{1,18}(ANNEX|EXHIBIT|SCHEDULE|APPENDIX|Annex|Exhibit|Schedule|Appendix).{1,3}[A-Z\\d- ]{1,7}))");

	public static Pattern patternEndOfContract = Pattern
			.compile(
//					patternClosingPara.pattern() + "|" + 
			patternExhibitHeader.pattern());

	public static Pattern patternClosingPara2 = Pattern.compile("SIGNATURES");

	public static Pattern patternEndOfContract2 = Pattern
			.compile(
//					patternClosingPara.pattern() + "|" + 
			patternClosingPara2.pattern());

	// sometimes only 1 hard return
	public static Pattern patternSignaturePage = Pattern.compile("(?sm)[\r\n]{1,} ?\\|?"
			+ "(?=(Signature.{1,4}Page.{1,4}Follows|SIGNATURE.{1,4}PAGE.{1,4}FOLLOWS) ?\\|? ?)");

	public static Pattern patternEndOfContractLessRestritive = Pattern.compile(
//			patternClosingPara.pattern() + "|" + 
	patternExhibitHeader.pattern() + "|" + patternSignaturePage.pattern());

	public static void main(String[] args) throws IOException {

	}

}
