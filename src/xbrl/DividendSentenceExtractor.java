package xbrl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;

public class DividendSentenceExtractor {

	public static Pattern PayableDateBeforePattern = Pattern
			.compile("(?<=( be distributed[ \\xA0]{0,2}| payment date[ \\xA0]{0,2}| be paid[ \\xA0]{0,2}| payable[ \\xA0]{0,2}(date[ \\xA0]{0,2})?)( to.{5,8}holders[ \\xA0]{0,2})?( in cash[ \\xA0]{0,2})?(on or before[ \\xA0]{0,2}| on or about[ \\xA0]{0,2}| on[ \\xA0]{0,2}| is[ \\xA0]{0,2}| of[ \\xA0]{0,2}| will be[ \\xA0]{0,2}|[ \\xA0]{0,2}))((([a-zA-Z]{3,9}[^\\p{ASCII}|[\\. ]]{1,5}\\d{1,2}[thsnrd]{0,2},[^\\p{ASCII}|[ \\xA0]]{1,5}\\d{4})|([\\xA0 ]{1}\\d{1,2}[thsnrd]{0,2}[^\\p{ASCII}|[ ]]{1,5}(Jan[ \\.\\xA0]|January|Feb[ \\.\\xA0]|February|Mar[ \\.\\xA0]|March|Apr[ \\.\\xA0]|April|May|Jun[ \\.\\xA0]|June|Jul[ \\.\\xA0]|July|Aug[ \\.\\xA0]|August|Sep[ \\.\\xA0]|September|Oct[ \\.\\xA0]|October|Nov[ \\.\\xA0]|November|Dec[ \\.\\xA0]|December)[^\\p{ASCII}|[ \\.]]{1,5}\\d{4})))");
	// picks either US or Euro style date pattern (non-capturing groups used)

	public static Pattern PayableDateAfterPattern = Pattern
			.compile("(Jan[ \\.\\xA0]|January|Feb[ \\.\\xA0]|February|Mar[ \\.\\xA0]|March|Apr[ \\.\\xA0]|April|May" +
					"|Jun[ \\.\\xA0]|June|Jul[ \\.\\xA0]|July[ \\xA0]|Aug[ \\.\\xA0]|August|Sep[ \\.\\xA0]|September" +
					"|Oct[ \\.\\xA0]|October|Nov[ \\.\\xA0]|November|Dec[ \\.\\xA0]|" +
					"December)[ \\xA0]{0,2}[0-9]{1,2}[\\, \\xA0]{0,3}\\d{4}" +
					"[\\, \\xA0]{0,3}(?=(( the Company)?( it)?( will)?( pay | payment)))");
	// picks US date patterns only

	public static Pattern PayableDatePattern = Pattern
			.compile(PayableDateBeforePattern + "|" + PayableDateAfterPattern);

	public static Pattern RecordDatePattern = Pattern
			.compile("(?<=( record[ \\xA0]{0,2}date.{0,4}| of[ \\xA0]{1,2}record[ \\xA0]{0,2}| on[ \\xA0]{1,2}record[ \\xA0]{0,2})(of (the )? ([Cc]ompany.{1,3})?common stock )?(as of[ \\xA0]{0,2}|at[ \\xA0]{0,2})?((the )?close of business[ \\xA0]{0,2})?(of )?(will be[ \\xA0]{0,2})?(on[ \\xA0]{0,2})?)((([a-zA-Z]{3,9}[^\\p{ASCII}|[\\. ]]{1,5}\\d{1,2}[thsnrd]{0,2},[^\\p{ASCII}|[ ]]{1,5}\\d{4})|([\\xA0 ]{1}\\d{1,2}[thsnrd]{0,2}[^\\p{ASCII}|[ ]]{1,5}(Jan[ \\.\\xA0]|January|Feb[ \\.\\xA0]|February|Mar[ \\.\\xA0]|March|Apr[ \\.\\xA0]|April|May[ \\xA0]|Jun[ \\.\\xA0]|June|Jul[ \\.\\xA0]|July|Aug[ \\.\\xA0]|August|Sep[ \\.\\xA0]|September|Oct[ \\.\\xA0]|October|Nov[ \\.\\xA0]|November|Dec[ \\.\\xA0]|December)[^\\p{ASCII}|[, \\.][0-9]]{1,5}\\d{4})))");
	// match record with US and Euro Date Pattern. 2nd date pattern is euro and

	public static Pattern RecordDateSimplePattern = Pattern
			.compile("(record[ \\xA0]{0,2}date.{0,4}| of[ \\xA0]{1,2}record[ \\xA0]{0,2}| on[ \\xA0]{1,2}record[ \\xA0]{0,2}).{1,70}((([a-zA-Z]{3,9}[^\\p{ASCII}|[\\. ]]{1,5}\\d{1,2}[thsnrd]{0,2},[^\\p{ASCII}|[ ]]{1,5}\\d{4})|([\\xA0 ]{1}\\d{1,2}[thsnrd]{0,2}[^\\p{ASCII}|[ ]]{1,5}(Jan[ \\.\\xA0]|January|Feb[ \\.\\xA0]|February|Mar[ \\.\\xA0]|March|Apr[ \\.\\xA0]|April|May[ \\xA0]|Jun[ \\.\\xA0]|June|Jul[ \\.\\xA0]|July|Aug[ \\.\\xA0]|August|Sep[ \\.\\xA0]|September|Oct[ \\.\\xA0]|October|Nov[ \\.\\xA0]|November|Dec[ \\.\\xA0]|December)[^\\p{ASCII}|[, \\.][0-9]]{1,5}\\d{4})))");

	public static Pattern GetRecordDateFromSimplePattern = Pattern
			.compile("(?<=([ \\xA0]{1,2}record.{1,100}))((([a-zA-Z]{3,9}[^\\p{ASCII}|[\\. ]]{1,5}\\d{1,2}[thsnrd]{0,2},[^\\p{ASCII}|[ ]]{1,5}\\d{4})|([\\xA0 ]{1}\\d{1,2}[thsnrd]{0,2}[^\\p{ASCII}|[ ]]{1,5}(Jan[ \\.\\xA0]|January|Feb[ \\.\\xA0]|February|Mar[ \\.\\xA0]|March|Apr[ \\.\\xA0]|April|May[ \\xA0]|Jun[ \\.\\xA0]|June|Jul[ \\.\\xA0]|July|Aug[ \\.\\xA0]|August|Sep[ \\.\\xA0]|September|Oct[ \\.\\xA0]|October|Nov[ \\.\\xA0]|November|Dec[ \\.\\xA0]|December)[^\\p{ASCII}|[, \\.][0-9]]{1,5}\\d{4})))");

	public static Pattern StockTickerPattern = Pattern
			.compile("(?<=(\\((?i)(NASDAQ G[SM]{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NASDAQ GSM{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NASD GM{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NASDAQ{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NASD{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(OTCBB{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(OTC{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NYSE{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NYSE MKT{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "| (?<=(\\((?i)(AMEX{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(PINK SHEETS{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(NQB{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(TSX{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(LSE{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(?i)(NYSE AMEX{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(?i)(NYSE ARCA{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}"
					+ "|(?<=(\\((?i)(OTC Bulletin Board{1,2}[: ]{1,2})))[A-Za-z]{1,9}[, A-Z]{3,7}");

	public static Pattern DividendPattern = Pattern
			.compile("[Dd]ividend[s\\. ]{1}|[Dd]istribution[s\\. ]{1}");

	public static Pattern SharePattern = Pattern
			.compile("[Ss]hare[\\. ]{1}|[Ss]tock[\\. ]{1}|[Uu]nit[\\. ]{1}|[Pp]referred[\\. ]{1}|[Cc]ommon[\\. ]{1}|[Dd]eposit[ao]ry[\\. ]{1}|[Tr]rust[\\. ]{1}|[Pp]artnership[\\. ]{1}|[Ll]\\.{0,1}[Pp][Cc]onvertible[\\. ]{1}|[Cc]umulative");

	public static Pattern GenealFalsePositives = Pattern
			.compile("[Ss]plit|[Mm]erger|[Rr]ight.{1,4}([Oo]ffering)?([Aa]greement)?|([Ss]pin?)(.{1,2}off)?|(plan)?");
	// fix - ony gets two characters of 2nd workd - right agh

	public static Pattern ClassPattern = Pattern
			.compile(" [Cc]lass [A-Z]{1,2}\\s| [Ss]eries [A-Z]{1,2}\\s");

	public static Pattern FPbeforeAmt$Pattern = Pattern
			.compile("((equate.{0,2}( to)?( an)?( annual.{0,4})?( rate)?| equivalent( to)?| decreas| increas)[ eding]{0,3}( approximately| by| of| is| would be)?| par value| are| from| compared( to)?( the)?| aggregate of| than( the)?)\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1: equiv|incr|decr approx/to/by/of grp2: are / from / comp to/aggre of

	public static Pattern FPbeforeAmt2$Pattern = Pattern
			.compile("( previous.{0,3}| prior| compared to a)( declar.{1,3})?( annual)?( quarter.{0,3})?( cash)?( dividend)?( distribution)?( payment)?( level)?( of)?[ ]{1,4}\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1. previous/prior/compared to a ==> ann|qtr|cash|div|dis|paym|lvl|of

	public static Pattern FPbeforeAmt3$Pattern = Pattern
			.compile("([Tt]otal| indicat.{1,5}| equivalent| previous| represent.{2,6})( [Aa]nnual.{0,5})?( [Dd]ividend)?( [Dd]istribution)?( amount)?( payment)?( [Rr]ate)?( of| is| would be)? \\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1:ann==>div/dis/pay/rate/amt/of is

	public static Pattern FPbeforeAmt4$Pattern = Pattern
			.compile("( equal( a)?|total.{0,4}|(annual.{0,4}( basis.{1,9})?( represents.{1,6})?( cash)?( dividend)?)( annual)?( payout)?( of)?)\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1: equal a/ total/ annual

	public static Pattern FPbeforeAmt5$Pattern = Pattern
			.compile("( increas.{0,3}| decreas.{0,3})( from| over)?( its| the)?( previous.{0,3})?( prior)?( initial)?( fourth)?( quarter.{0,3})?( 19[\\d]{2}| 20[\\d]{2})?( cash)?( dividend.{0,3}| distribution.{0,3})?( rate)?( of| the)? \\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1: increas/decrea over/prior/prev qtr div

	public static Pattern FPbeforeAmt6$Pattern = Pattern
			.compile("(represent[s]{0,1} )(a[n]{0,1} )?(increase )?(of [.,\\d]{0,6})?([%][, ]{1,3}|percent[, ]{1,3})?(or )?\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er |on each |for each |a share )");
	// grp1: represents increase of x%, or $x per sh

	public static Pattern FPbeforeAmt7$Pattern = Pattern
			.compile("(returned|annual[ized]{0,4}| inception.{1,6})( rate?( the)?| basis[,]{0,1})?( the)?( cash)?( dividend| distribution)?( rate)?( amount)?( is)?( current[ly]{0,2})?( reflects| expect[ed]{0,2})?( to)?( of)?( be)?.{0,4}\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1: annual basis/curr/expect

	public static Pattern FPbeforeAmt8$Pattern = Pattern
			.compile("(cumulatively)?( distributed) \\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// distributed

	public static Pattern FPbeforeAmt9$Pattern = Pattern
			.compile("([Tt]otal)( year.{1}end)?( [\\d]{4})?( declar.{1,4}| announc.{1,4})?( distribution.{0,1}| dividend.{0,1})?( of| to) \\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");
	// grp1: Ttotal decl/ann yyyy...

	public static Pattern FPbeforeAmt10$Pattern = Pattern
			.compile("( record{0,2}| estimat.{1,4}|[Bb]ook [Vv]alue| for the year| plus| consist.{0,4}| earn.{0,5}| [Tt]otal year.{1}end| previous)( distribution)?( of| to| the| approximately)?\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}[Pp]er[\\s]");

	public static Pattern FPbeforeAmt11$Pattern = Pattern
			.compile("(paid in )?(?<!([JFMAJASOND]{1}).{5,15})[1-2]{1}[0|9]{1}[0-9]{1}[0-9]{1}.{0,5}(full )?(year )?(to )?\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[\\s]{1,2}([Pp]er[\\s]|for each[\\s]|on each[\\s]|a share )");

	public static Pattern FPbeforeAmt12$Pattern = Pattern
			.compile("( full year.{0,6}| represent.{1,11}increa.{1,11}(percent.{1,9})?| expect.{1,9}remain.{1,9}| guidance.{0,6})((\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[\\s]{1,2}?<!(cent[s]))|\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[\\s]{1,2}([Pp]er[\\s]|for each[\\s]|on each[\\s]|a share ))");

	public static Pattern FPafterAmt$Pattern = Pattern
			.compile("\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er |on each |a share |for each )(outstanding[, ]{1,2})?([Pp]referred[, ]{1,2})?([Ss]ubordinat.{1,3}[, ]{1,2})?([Cc]ommon[, ]{1,2})?([Pp]artnership[, ]{1,2})?([Tt]rust[, ]{1,2})?([LPlp][, ]{1,2})?([Dd]eposit[oa]ry[, ]{1,2})?([Dd]iluted[, ]{1,2})?([Cc]onvertible[, ]{1,2})?([Nn]on.{1}[, ]{1,2})?([Cc]umulative[, ]{1,2})?([Rr]edeemable[, ]{1,2})?([Ss]eries [A-Z]{1,2}[, ]{1,2})?([Cc]lass [A-Z]{1,2}[, ]{1,2})?([Ss]hare[, ]{1,2})?([Ss]tock[, ]{1,2})?([Uu]nit[, ]{1,2})?(on )?(an )?(annual.{0,5}|over )");
	// grp1: annual./over
	public static Pattern FPafterAmt2$Pattern = Pattern
			.compile("\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er |on each |a share |for each )((outstanding)?(preferred)?(subordinat.{1,3})?(common)?(partnership)?(trust)?([LPlp])?(deposit[oa]ry)?(diluted)?(convertible)?(non.{1})?(cumulative)?(redeemable)?(Series [A-Z]{1,2} )?(Class [A-Z]{1,2} )?(share)?(stock)?(unit)?)[,.\\s]{0,6}((increas.{1,4})|( or [.\\s]{1,7})|(higher )|((long.{0,3}|short.{0,3})term( capital)? gain)|( in )?[1-2]{1}[0|9]{1}[0-9]{1}[0-9]{1})");
	// is $0.01 per share, or 5.9%, higher

	public static Pattern FPafterAmt3$Pattern = Pattern
			.compile("\\s\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er year)");

	public static Pattern FPPaidBeforeAmt$Pattern = Pattern
			.compile("(?<!be )(paid |were |was )(a )?(an )?(in )?(quarter.{1,3})?(common )?(stock )?(cash )?(special )?(dividend.{0,1} |distribution{0,1} )?(at )?(the )?(rate )?(of )?\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er |on each |a share |for each )");

	public static Pattern FPPaidAfterAmt$Pattern = Pattern
			.compile("\\$?[\\d]{0,4}\\.[\\d]{1,6}[)]{0,1}[\\s]{1,2}([Pp]er )?(common share )?(outstanding )?(share )?(of )?(common )?(stock )?(share )?(in )?(cash )?(dividend.{0,1} )?(distribution.{0,1} )?(that )?(was )?(were )?(paid )");
	// paid prececded by either (per, comm
	// sh/outst/share/of/comm/stock/sh/in/cash/div/that/was/...

	public static Pattern FalsePostiveAmt$Patterns = Pattern
			.compile(FPbeforeAmt$Pattern + "|" + FPbeforeAmt2$Pattern + "|"
					+ FPbeforeAmt3$Pattern + "|" + FPbeforeAmt4$Pattern + "|"
					+ FPbeforeAmt5$Pattern + "|" + FPbeforeAmt6$Pattern + "|"
					+ FPbeforeAmt7$Pattern + "|" + FPbeforeAmt8$Pattern + "|"
					+ FPbeforeAmt9$Pattern + "|" + FPbeforeAmt10$Pattern + "|"
					+ FPbeforeAmt11$Pattern + "|" + FPbeforeAmt12$Pattern + "|"
					+ FPafterAmt$Pattern + "|" + FPafterAmt2$Pattern + "|"
					+ FPafterAmt3$Pattern + "|" + FPPaidBeforeAmt$Pattern + "|"
					+ FPPaidAfterAmt$Pattern);

	public static Pattern FPbeforeAmtCentsPattern = Pattern
			.compile("((equate( to)?| equivalent( to)?| decreas| increas)[eding ]{0,3}( approximately| by| of| is| would be)?| par value| are| from| compared( to)?( the)?| aggregate of| than( the)?).{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: equiv|incr|decr approx/to/by/of grp2: are / from / comp to/aggre of

	public static Pattern FPbeforeAmt2CentsPattern = Pattern
			.compile("( previous.{0,3}| prior| compared to a)( declar.{1,3})?( annual)?( quarter.{0,3})?( cash)?( dividend)?( distribution)?( payment)?( level)?( of)?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1. previous/prior/compared to a ==> ann|qtr|cash|div|dis|paym|lvl|of

	public static Pattern FPbeforeAmt3CentsPattern = Pattern
			.compile("([Tt]otal| indicat.{1,5}| equivalent| previous| represent.{2,6})( [Aa]nnual.{0,5})?( [Dd]ividend)?( [Dd]istribution)?( amount)?( payment)?( [Rr]ate)?( of| is| would be)?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1:ann==>div/dis/pay/rate/amt/of is

	public static Pattern FPbeforeAmt4CentsPattern = Pattern
			.compile("( equal( a)?|total.{0,4}| annual)( annual)?( payout)?( of)?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: equal a/ total/ annual
	public static Pattern FPbeforeAmt5CentsPattern = Pattern
			.compile("( increas.{0,3}| decreas.{0,3})( from| over)?( its| the)?( 	s.{0,3})?( prior)?( initial)?( fourth)?( quarter.{0,3})?( 19[\\d]{2}| 20[\\d]{2})?( cash)?( dividend.{0,3}| distribution.{0,3})?( rate)?( of| the)?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: increas/decrea over/prior/prev qtr div

	public static Pattern FPbeforeAmt6CentsPattern = Pattern
			.compile("(represent[s]{0,1} )(a[n]{0,1} )?(increase )?(of [.,\\d]{0,6})?([%][, ]{1,3}|[, ]{1,3})?(or )?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: represents increase of x%, or $x [Pp]er sh

	public static Pattern FPbeforeAmt7CentsPattern = Pattern
			.compile("(returned|annual[ized]{0,4}| inception.{1,6})( rate?( the)?| basis[,]{0,1})?( the)?( cash)?( dividend| distribution)?( rate)?( amount)?( is)?( current[ly]{0,2})?( reflects| expect[ed]{0,2})?( to)?( of)?( be)?.{0,4} ((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: annual basis/curr/expect

	public static Pattern FPbeforeAmt8CentsPattern = Pattern
			.compile("(cumulatively)?( distributed).{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// distributed

	public static Pattern FPbeforeAmt9CentsPattern = Pattern
			.compile("([Tt]otal)( year.{1}end)?( [\\d]{4})?( declar.{1,4}| announc.{1,4})?( distribution.{0,1}| dividend.{0,1})?( of| to).{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");
	// grp1: Ttotal decl/ann yyyy...

	public static Pattern FPbeforeAmt10CentsPattern = Pattern
			.compile("( record{0,2}| estimat.{1,4}|[Bb]ook [Vv]alue| for the year| plus| consist.{0,4}| [Ii]nvestment [Ii]ncome| earn.{0,5}| [Tt]otal year.{1}end| previous)( distribution)?( of| to| the| approximately)?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");

	public static Pattern FPbeforeAmt11CentsPattern = Pattern
			.compile(" paid in [1-2]{1}[0|9]{1}[0-9]{1}[0-9]{1}.{0,5}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| two cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| three cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er| four cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | five cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | six cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eight cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nine cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | ten cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eleven cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twelve cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | thirteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fourteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | fifteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | sixteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | seventeen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | eighteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | nineteen cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er | twenty cents [(]\\$?\\d{0,3}\\.\\d{0,6}[)] [Pp]er))");

	public static Pattern FPafterAmtCentsPattern = Pattern
			.compile("((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [Pp]er| two cents [Pp]er| three cents [Pp]er| four cents [Pp]er | five cents [Pp]er | six cents [Pp]er | seven cents [Pp]er | eight cents [Pp]er | nine cents [Pp]er | ten cents [Pp]er | eleven cents [Pp]er | twelve cents [Pp]er | thirteen cents [Pp]er | fourteen cents [Pp]er | fifteen cents [Pp]er | sixteen cents [Pp]er | seventeen cents [Pp]er | eighteen cents [Pp]er | nineteen cents [Pp]er | twenty cents [Pp]er)[\\s])(outstanding[, ]{1,2})?([Pp]referred[, ]{1,2})?([Ss]ubordinat.{1,3}[, ]{1,2})?([Cc]ommon[, ]{1,2})?([Pp]artnership[, ]{1,2})?([Tt]rust[, ]{1,2})?([LPlp][, ]{1,2})?([Dd]eposit[oa]ry[, ]{1,2})?([Dd]iluted[, ]{1,2})?([Cc]onvertible[, ]{1,2})?([Nn]on.{1}[, ]{1,2})?([Cc]umulative[, ]{1,2})?([Rr]edeemable[, ]{1,2})?([Ss]eries [A-Z]{1,2}[, ]{1,2})?([Cc]lass [A-Z]{1,2}[, ]{1,2})?([Ss]hare[, ]{1,2})?([Ss]tock[, ]{1,2})?([Uu]nit[, ]{1,2})?(on )?(an )?(annual.{0,4}|over )");
	// grp1: annual./over
	public static Pattern FPafterAmt2CentsPattern = Pattern
			.compile("((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [Pp]er| two cents [Pp]er| three cents [Pp]er| four cents [Pp]er | five cents [Pp]er | six cents [Pp]er | seven cents [Pp]er | eight cents [Pp]er | nine cents [Pp]er | ten cents [Pp]er | eleven cents [Pp]er | twelve cents [Pp]er | thirteen cents [Pp]er | fourteen cents [Pp]er | fifteen cents [Pp]er | sixteen cents [Pp]er | seventeen cents [Pp]er | eighteen cents [Pp]er | nineteen cents [Pp]er | twenty cents [Pp]er)[\\s])((outstanding)?(preferred)?(subordinat.{1,3})?(common)?(partnership)?(trust)?([LPlp])?(deposit[oa]ry)?(diluted)?(convertible)?(non.{1})?(cumulative)?(redeemable)?(Series [A-Z]{1,2} )?(Class [A-Z]{1,2} )?(share)?(stock)?(unit)?)[,.\\s]{0,6}((increas.{1,4})|( or [.\\s]{1,7})|(higher )|((long.{0,3}|short.{0,3})term( capital)? gain))");
	// is $0.01 [Pp]er share, or 5.9%, higher

	public static Pattern FPPaidBeforeAmtCentsPattern = Pattern
			.compile("(?<!be )(paid |were |was )(a )?(an )?(in )?(quarter.{1,3})?(common )?(stock )?(cash )?(special )?(dividend.{0,1} |distribution{0,1} )?(at )?(the )?(rate )?(of )?.{0,4}((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [Pp]er| two cents [Pp]er| three cents [Pp]er| four cents [Pp]er | five cents [Pp]er | six cents [Pp]er | seven cents [Pp]er | eight cents [Pp]er | nine cents [Pp]er | ten cents [Pp]er | eleven cents [Pp]er | twelve cents [Pp]er | thirteen cents [Pp]er | fourteen cents [Pp]er | fifteen cents [Pp]er | sixteen cents [Pp]er | seventeen cents [Pp]er | eighteen cents [Pp]er | nineteen cents [Pp]er | twenty cents [Pp]er))");

	public static Pattern FPPaidAfterAmtCentsPattern = Pattern
			.compile("((\\${0,1}[\\d]{0,4}\\.?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}[Pp]er[\\s])|( one cent [Pp]er| two cents [Pp]er| three cents [Pp]er| four cents [Pp]er | five cents [Pp]er | six cents [Pp]er | seven cents [Pp]er | eight cents [Pp]er | nine cents [Pp]er | ten cents [Pp]er | eleven cents [Pp]er | twelve cents [Pp]er | thirteen cents [Pp]er | fourteen cents [Pp]er | fifteen cents [Pp]er | sixteen cents [Pp]er | seventeen cents [Pp]er | eighteen cents [Pp]er | nineteen cents [Pp]er | twenty cents [Pp]er))(common share )?(outstanding )?(share )?(of )?(common )?(stock )?(share )?(in )?(cash )?(dividend.{0,1} )?(distribution.{0,1} )?(that )?(was )?(were )?(paid )");
	// paid prececded by either (per, comm
	// sh/outst/share/of/comm/stock/sh/in/cash/div/that/was/...

	public static Pattern FalsePostiveAmtCentPatterns = Pattern
			.compile(FPbeforeAmtCentsPattern + "|" + FPbeforeAmt2CentsPattern
					+ "|" + FPbeforeAmt3CentsPattern + "|"
					+ FPbeforeAmt4CentsPattern + "|" + FPbeforeAmt5CentsPattern
					+ "|" + FPbeforeAmt6CentsPattern + "|"
					+ FPbeforeAmt7CentsPattern + "|" + FPbeforeAmt8CentsPattern
					+ "|" + FPbeforeAmt9CentsPattern + "|"
					+ FPbeforeAmt10CentsPattern + "|"
					+ FPbeforeAmt11CentsPattern + "|" + FPafterAmtCentsPattern
					+ "|" + FPafterAmt2CentsPattern + "|"
					+ FPPaidBeforeAmtCentsPattern + "|"
					+ FPPaidAfterAmtCentsPattern);

	public static Pattern Amount1$Pattern = Pattern
			.compile("(?<=((declared a[\\s]{0,3})(regular )?(monthly |quarterly )?(cash )?(common )?(share )?([Dd]ividend of[ \\xA0]{1,2}|[Dd]istribution of[ \\xA0]{1,2})))(\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[\\s]{1,2}?<!(cent[s]))|\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[\\s]{1,2}([Pp]er[\\s]|for each[\\s]|on each[\\s]|a share )");

	public static Pattern Amount2$Pattern = Pattern
			.compile("(?<=((declared.{1,40}|announced.{1,40})((distribution |dividend )of )))\\$?[\\d]{0,4}\\.[\\d]{1,9}[)]{0,1}[.\\s]{1,3}(?!cent[s]{0,1})(?!per )");

	public static Pattern Amount$Pattern = Pattern.compile(Amount1$Pattern
			+ "|" + Amount2$Pattern);

	public static Pattern AmountCentPattern = Pattern
			.compile("(?<=((declared a[\\s]{0,3})(regular )?(monthly |quarterly )?(cash )?(common )?(share )?(dividend of[ \\xA0]{1,2}|distribution of[ \\xA0]{1,2})))(\\$?[\\d]{1,2}\\.[\\d]{1,6}[ \\xA0]{1,2}cent[s]{1}[ \\xA0]{1,2})|((\\${0,1}[\\d]{0,4}[\\s]{0,1}[\\-12\\/\\.]{0,3}?[\\d]{0,6}[)]?[\\s]{1,2}cent[s]{0,1}[\\s]{1,2}per[\\s])|( one cent per| two cents per| three cents per| four cents per | five cents per | six cents per | seven cents per | eight cents per | nine cents per | ten cents per | eleven cents per | twelve cents per | thirteen cents per | fourteen cents per | fifteen cents per | sixteen cents per | seventeen cents per | eighteen cents per | nineteen cents per | twenty cents per))");
	// DOES NOT CAPTURE 31 ½ cents per share 31 ¼ cents per share

	public static Pattern FrequencyPattern = Pattern
			.compile("(?i)( accrued[ \\.]{1}| quarter[ \\.]{1}| quarterly[ \\.]{1}|catch.{0,1}up| special[ \\.]{1}| extraordinary[ \\.]{1}| one.{0,1}time[ \\.]{1}| semi.{0,1}(annual[ \\.]{1}|annually[ \\.]{1})| bi.{0,1}(annual[ \\.]{1}|annually[ \\.]{1})| per annum[ \\.]{1}| annual[ \\.]{1}| annually[ \\.]{1}| year[ \\.]{1}| yearly[ \\.]{1}| month[ \\.]{1}| monthly[ \\.]{1})|(extra |partial )(quarterly )?(cash )?(dividend|distribution)");

	public static int PaytoRecDistance = 700;

	public static void getTextBlock(String text, String acceptedDate,
			String acc, String exhibit8kor99) throws FileNotFoundException {
		
		NLP nlp = new NLP();
		text = text.replaceAll("\r\n", " ").replaceAll("  ", "");

		
		Map<Integer, List<Integer>> mapIndices = new HashMap<Integer, List<Integer>>();
		List<Integer> value = null;

		mapIndices.put(1, nlp.getAllIndexStartLocations(text
//										.replaceAll(
//												"Monday[, ]|Tuesday[, ]|Wednesday[, ]|Thursday[, ]|Friday[, ]",
//												"")
, PayableDatePattern));
		 
		System.out.println("payDt: "+mapIndices.toString());
		 Matcher matcherP = PayableDatePattern.matcher(text);
		 while(matcherP.find()){
			 System.out.println("matcherPayDt="+matcherP.group());
		 }

		 
		mapIndices
				.put(2, nlp.getAllIndexStartLocations(text,
						RecordDatePattern));

		Matcher matcherR = RecordDatePattern.matcher(text);
		 while(matcherR.find()){
			 matcherR.group();
			 System.out.println("matcherRecDt="+matcherR.group());
		 }


		
		if (mapIndices.get(2).size() == 0 || mapIndices.get(2) == null)
			mapIndices.put(2, nlp.getAllIndexStartLocations(text,
					RecordDateSimplePattern));

		 System.out.println("recDt: "+mapIndices.get(2).size());
		if (mapIndices.get(1) == null || mapIndices.get(1).size() == 0
				|| mapIndices.get(2) == null || mapIndices.get(2).size() == 0)
			return;

		// System.out.println("before:" + mapIndices.size());
		// for (List<Integer> list : mapIndices.values())
		// System.out.println("before:" + Arrays.toString(list.toArray()));

		value = getClosestIndicesFromFirstList(mapIndices.get(1),
				mapIndices.get(2), PaytoRecDistance);
		if (null == value || value.size() == 0)
			return;
		mapIndices.put(1, value);

		value = getClosestIndicesFromFirstList(mapIndices.get(2),
				mapIndices.get(1), PaytoRecDistance);
		if (null == value || value.size() == 0)
			return;
		mapIndices.put(2, value);
		// may need to create loop if there is more than 1 proximity group.

		// System.out.println("after:" +
		// Arrays.toString(mapIndices.get(1).toArray()));
		// System.out.println("after:" +
		// Arrays.toString(mapIndices.get(2).toArray()));

		int minIdx = Math.min(mapIndices.get(1).get(0), mapIndices.get(2)
				.get(0));
		int maxIdx = Math.max(mapIndices.get(1).get(0), mapIndices.get(2)
				.get(0));
		text = text.replaceAll("\t", " ");
		text = text.replaceAll("[\r\n]", " ");

		text = text.substring(Math.max(0, minIdx - 2000),
				Math.min(maxIdx + 1700, text.length()));
		 System.out.println("this is textBlock:: " + text);
		Utils.createFoldersIfReqd(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/Text");
		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/Text/" + acc + "_" + exhibit8kor99 + ".txt");

		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		PrintWriter out;
		try {
			out = new PrintWriter(
					new BufferedWriter(new FileWriter(file, true)));
			out.println(acceptedDate + "||" + acc + "||" + text + "||"
					+ exhibit8kor99);
			out.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		String filename = file.getAbsolutePath().replace("\\", "/");

		loadIntoMysql(filename, "99text");
		patternsToMatch(text, acceptedDate, acc, exhibit8kor99);
		System.out.println(filename);
	}

	public static void patternsToMatch(String text, String acceptedDate,
			String acc, String exhibit8kor99) throws FileNotFoundException {

		NLP nlp = new NLP();
		
		List<Integer> sentenceLocs = nlp.getSentenceStartLocations(text);

		matchPattern(RecordDatePattern, text, acceptedDate, acc, "Rec",
				sentenceLocs, exhibit8kor99);
		matchPattern(RecordDateSimplePattern, text, acceptedDate, acc, "RecS",
				sentenceLocs, exhibit8kor99);
		matchPattern(PayableDatePattern, text, acceptedDate, acc, "Pay",
				sentenceLocs, exhibit8kor99);
		matchPattern(Amount$Pattern, text, acceptedDate, acc, "Amt",
				sentenceLocs, exhibit8kor99);
		matchPattern(AmountCentPattern, text, acceptedDate, acc, "AmtC",
				sentenceLocs, exhibit8kor99);
		matchPattern(StockTickerPattern, text, acceptedDate, acc, "Ticker",
				sentenceLocs, exhibit8kor99);
	}

	public static void matchPattern(Pattern ptrn, String text,
			String acceptedDate, String acc, String type,
			List<Integer> sentenceLocs, String exhibit8kor99) throws FileNotFoundException {

		String match = null;
		int matchLoc = 0;
		// String subText = null;
		Matcher matchPattern = ptrn.matcher(text);
		// apply pattern to text

		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + type);

		if (file.exists()) {
			file.delete();
		}

		try {
			file.createNewFile();
			PrintWriter out2 = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));
			String keyword = null;
			int sentSLoc = 0;
			while (matchPattern.find()) {
				System.out.println("Ptrn: " + type + "::"
						+ matchPattern.group());
				match = matchPattern.group();
				keyword = match;
				// System.out.println(type + " :: " + matchPattern.group());
				matchLoc = matchPattern.start();
				// size/length: gives actual no of items (e.g., 1,2, or 3). But
				// to call from an array you start from 0.
				for (sentSLoc = 0; sentSLoc < sentenceLocs.size(); sentSLoc++) {
					// this checks all sentences
					if (sentenceLocs.get(sentSLoc) > matchLoc)
						// stops right before the beginning of match sentence.
						break;
				}

				int sentEndLoc = text.length();
				// fail safe so no out of bounds errors
				if (sentSLoc < sentenceLocs.size())
					sentEndLoc = sentenceLocs.get(sentSLoc);
				// end is begging of sentLoc
				// System.out.println("sentence end locs: " + sentEndLoc);
				String sentence = "";
				int sentStartLoc = 0;
				if (sentSLoc - 1 >= 0) {
					sentence = text.substring(sentenceLocs.get(sentSLoc - 1),
							sentEndLoc);
					sentStartLoc = sentenceLocs.get(sentSLoc - 1);
				}

				// need to set sentStartLoc if the if condition above is
				// returned as well.
				// more fundamentally need to not 'reparse' text when using
				// parse text
				// System.out.print("sentence: " + sentMatch);
				// subText = text.substring(
				// Math.max(0, matchPattern.start() - 30),
				// Math.min(matchPattern.start() + 45, text.length()));
				// System.out.println("subtext::" + subText);

				if (type == "Rec" || type == "Pay" || type == "Amt"
						|| type == "AmtC" || type == "Ticker") {

					String data = (acceptedDate + "||" + acc + "||" + match
							+ "||" + matchLoc + "||" + sentStartLoc + "||"
							+ sentEndLoc + "||" + sentence + "||"
							+ exhibit8kor99 + "\n");
					System.out.println("data="+data);

					out2.append(data);
					String filename = file.getAbsolutePath().replace("\\", "/");
					loadIntoMysql(filename, "99" + type);

					getWordsBeforeMatch(acceptedDate, acc, match, matchLoc,
							sentStartLoc, sentEndLoc, sentence, type,
							exhibit8kor99, keyword);
					getWordsAfterMatch(acceptedDate, acc, match, matchLoc,
							sentStartLoc, sentEndLoc, sentence, type,
							exhibit8kor99, keyword);
				}
				if (type == "Pay" || type == "Rec" || type == "Amt"
						|| type == "AmtC") {
					out2.close();
					matchJustSentenceText(acceptedDate, acc, "Fre",
							sentStartLoc, sentEndLoc, sentence,
							FrequencyPattern, exhibit8kor99);
					matchJustSentenceText(acceptedDate, acc, "Div",
							sentStartLoc, sentEndLoc, sentence,
							DividendPattern, exhibit8kor99);
					matchJustSentenceText(acceptedDate, acc, "Share",
							sentStartLoc, sentEndLoc, sentence, SharePattern,
							exhibit8kor99);
					matchJustSentenceText(acceptedDate, acc, "Class",
							sentStartLoc, sentEndLoc, sentence, ClassPattern,
							exhibit8kor99);
				}
				// System.out.print("filename: " + filename + " ");
				if (type == "Amt") {
					out2.close();
					matchSubPattern(acceptedDate, acc, "fpAmt", sentStartLoc,
							sentEndLoc, sentence, FalsePostiveAmt$Patterns,
							Amount$Pattern, exhibit8kor99);
					matchAllinOneSentence(acceptedDate, acc, sentence,
							matchLoc, sentStartLoc, exhibit8kor99);
				}
				if (type == "AmtC") {
					out2.close();
					matchSubPattern(acceptedDate, acc, "fpAmtC", sentStartLoc,
							sentEndLoc, sentence, FalsePostiveAmtCentPatterns,
							AmountCentPattern, exhibit8kor99);
					matchAllinOneSentence(acceptedDate, acc, sentence,
							matchLoc, sentStartLoc, exhibit8kor99);
				}
				if (type == "RecS") {
					out2.close();
					matchJustSentenceText(acceptedDate, acc, "RecS",
							sentStartLoc, sentEndLoc, sentence,
							GetRecordDateFromSimplePattern, exhibit8kor99);
				}
			}
			out2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String filename = file.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99" + type);
		file.delete();
		System.out.print("filename: " + filename + " ");
	}

	public static void getWordsBeforeMatch(String acceptedDate, String acc,
			String match, int matchLoc, int sentStartLoc, int sentEndLoc,
			String sentence, String type, String exhibit8kor99, String keyword) throws FileNotFoundException {

		keyword = keyword.replaceAll("-", " ").replaceAll(",", "").trim();
		// System.out.println("keyword (before)::" + keyword);

		sentence = sentence.replaceAll("-", " ").replaceAll(",", "")
				.replace("%", "").replaceAll("'", "").replace(")", " ")
				.replaceAll("Inc.", "Inc").replace("(", " ")
				.replaceAll(":", " ").replaceAll("      ", " ")
				.replaceAll("    ", " ").replaceAll("    ", " ")
				.replaceAll("   ", " ").replaceAll("   ", " ")
				.replaceAll("  ", " ").replaceAll("  ", " ");

		Pattern WordsBeforeMatch = Pattern
				.compile("(\\s(([A-Za-z]|[\\d]|[$]|[\\.])+\\b)){0,10}\\s"
						+ "(?=(" + Pattern.quote(keyword) + "))");
		// (\\s(([A-Za-z]|[\\d]|[$]|[\\.])+\\b)){0,10}\\s
		// (?=(\\$17\\.0 cents per))
		// System.out.println("sentence (before)::" + sentence);

		Matcher matchWordsBeforeMatch = WordsBeforeMatch.matcher(sentence);

		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + type + "_Before");
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(
					file, true)));
			while (matchWordsBeforeMatch.find()) {
				// System.out.println("10 words before:: "
				// + matchWordsBeforeMatch.group());
				String mtch = matchWordsBeforeMatch.group().trim()
						.replaceAll(" ", "||");
				// System.out.println("wordsBefore (||)::" + mtch);

				if (acceptedDate != null && acc != null && matchLoc > 0
						&& sentStartLoc > 0 && sentEndLoc > 0
						&& sentence != null && exhibit8kor99 != null
						&& match != null && mtch != null)

					pw.append(acceptedDate + "||" + acc + "||" + matchLoc
							+ "||" + sentStartLoc + "||" + sentEndLoc + "||"
							+ sentence + "||" + exhibit8kor99 + "||" + match
							+ "||" + mtch + "\r\n");
			}
			pw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = file.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99" + type + "_Before");
		file.delete();
	}

	public static void getWordsAfterMatch(String acceptedDate, String acc,
			String match, int matchLoc, int sentStartLoc, int sentEndLoc,
			String sentence, String type, String exhibit8kor99, String keyword) throws FileNotFoundException {

		keyword = keyword.replaceAll("-", " ").replaceAll(",", "").trim();

		// System.out.println("sentence (After)::" + sentence);
		// System.out.println("keyword (After)::" + keyword);

		Pattern.quote(keyword);
		Pattern WordsAfterMatch = Pattern.compile("(?<=("
				+ Pattern.quote(keyword) + "))"
				+ "(\\s(([A-Za-z]|[\\d]|[$]|[\\.])+\\b)){0,10}\\s");
		// (?<=(\\$17\\.0 cents
		// per))(\\s(([A-Za-z]|[\\$]|[\\d]|[\\.])+\\b)){0,10}\\s
		sentence = sentence.replaceAll("-", " ").replaceAll(",", "")
				.replace("%", "").replaceAll("'", "").replace(")", " ")
				.replaceAll("Inc.", "Inc").replace("(", " ")
				.replaceAll(":", " ").replaceAll("      ", " ")
				.replaceAll("    ", " ").replaceAll("    ", " ")
				.replaceAll("   ", " ").replaceAll("   ", " ")
				.replaceAll("  ", " ").replaceAll("  ", " ");

		Matcher matchWordsAfterMatch = WordsAfterMatch.matcher(sentence);

		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + type + "_After");
		if (file.exists()) {
			file.delete();
		}
		try {
			file.createNewFile();
			PrintWriter pw2 = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));
			while (matchWordsAfterMatch.find()) {
				System.out.println("10 wordS After:: "
						+ matchWordsAfterMatch.group());
				String mtch = matchWordsAfterMatch.group().trim()
						.replaceAll(" ", "||");
				System.out.println("wordsAfter (||)::" + mtch);
				System.out.println(acceptedDate + "||" + acc + "||" + matchLoc
						+ "||" + sentStartLoc + "||" + sentEndLoc + "||"
						+ sentence + "||" + exhibit8kor99 + "||" + match + "||"
						+ mtch + "\r\n");

				if (acceptedDate != null && acc != null && matchLoc > 0
						&& sentStartLoc > 0 && sentEndLoc > 0
						&& sentence != null && exhibit8kor99 != null
						&& match != null && mtch != null)

					pw2.append(acceptedDate + "||" + acc + "||" + matchLoc
							+ "||" + sentStartLoc + "||" + sentEndLoc + "||"
							+ sentence + "||" + exhibit8kor99 + "||" + match
							+ "||" + mtch + "\r\n");
			}
			pw2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String filename = file.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99" + type + "_After");
		file.delete();
	}

	public static void matchJustSentenceText(String acceptedDate, String acc,
			String type, int sentStartLoc, int sentEndLoc, String sentence,
			Pattern Ptrn, String exhibit8kor99) throws FileNotFoundException {
		// match class/shareType/fre/div against pay/rec/amt sentence only.

		File file3 = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + type);
		if (file3.exists()) {
			file3.delete();
		}
		String mtch = null;
		try {
			file3.createNewFile();
			PrintWriter pw3 = new PrintWriter(new BufferedWriter(
					new FileWriter(file3, true)));
			Matcher match = Ptrn.matcher(sentence);
			while (match.find()) {
				mtch = match.group();
				String keyword = mtch;
				// System.out.println("fre str:" + mtch);
				int matchLoc = match.start() + sentStartLoc;
				pw3.append(acceptedDate + "||" + acc + "||" + mtch + "||"
						+ matchLoc + "||" + sentStartLoc + "||" + sentEndLoc
						+ "||" + sentence + "||" + exhibit8kor99 + "\r\n");

				getWordsBeforeMatch(acceptedDate, acc, mtch, matchLoc,
						sentStartLoc, sentEndLoc, sentence, type,
						exhibit8kor99, keyword);
				getWordsAfterMatch(acceptedDate, acc, mtch, matchLoc,
						sentStartLoc, sentEndLoc, sentence, type,
						exhibit8kor99, keyword);
			}
			pw3.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = file3.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99" + type);
		file3.delete();
	}

	public static void matchSubPattern(String acceptedDate, String acc,
			String type, int sentStartLoc, int sentEndLoc, String sentence,
			Pattern Ptrn, Pattern SubPattern, String exhibit8kor99) throws FileNotFoundException {

		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + type);
		// System.out.println("subPtrn type: " + type);

		if (file.exists()) {
			file.delete();
		}

		try {
			file.createNewFile();
			PrintWriter pw4 = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));
			Matcher match = Ptrn.matcher(sentence);
			// System.out.println("Ptrn Sentence: " + sentence);

			while (match.find()) {
				// System.out.println("subPtrn: " + match.group());
				String str = match.group();
				int matchLoc = match.start() + sentStartLoc;

				Matcher matchSubStr = SubPattern.matcher(str);
				while (matchSubStr.find()) {
					// System.out.println("subPtrn: " + matchSubStr.group());
					String mtch = matchSubStr.group();
					pw4.append(acceptedDate + "||" + acc + "||" + mtch + "||"
							+ matchLoc + "||" + sentStartLoc + "||"
							+ sentEndLoc + "||" + sentence + "||"
							+ exhibit8kor99 + "\r\n");
				}
			}
			pw4.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = file.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99" + type);
		file.delete();
	}

	public static void matchAllinOneSentence(String acceptedDate, String acc,
			String sentence, int matchLoc, int sentStartLoc,
			String exhibit8kor99) throws FileNotFoundException {

		Matcher matchPayableDate = PayableDatePattern.matcher(sentence);
		Matcher matchRecordDate = RecordDatePattern.matcher(sentence);
		Matcher match$Amount = Amount$Pattern.matcher(sentence);
		Matcher matchCentAmount = AmountCentPattern.matcher(sentence);
		Matcher matchFalsePositiveAmt$ = FalsePostiveAmt$Patterns
				.matcher(sentence);
		Matcher matchFalsePositivesCent = FalsePostiveAmtCentPatterns
				.matcher(sentence);

		Matcher matchDividendPattern = DividendPattern.matcher(sentence);

		File file = new File(EightK.getFolderForAcceptedDate(acceptedDate)
				+ "/" + acc + "99sent");
		// System.out.println("99sent file: " + file);

		if (file.exists()) {
			file.delete();
		}

		try {
			file.createNewFile();
			PrintWriter pw5 = new PrintWriter(new BufferedWriter(
					new FileWriter(file, true)));

			if ((match$Amount.find() || matchCentAmount.find())
					&& matchPayableDate.find() && matchRecordDate.find()
					&& !matchFalsePositiveAmt$.find()
					&& !matchFalsePositivesCent.find()
					&& matchDividendPattern.find()) {
				// System.out.println("all in one sentence");
				pw5.append(acceptedDate + "||" + acc + "||" + matchLoc + "||"
						+ sentStartLoc + "||" + sentence + "||" + exhibit8kor99
						+ "\r\n");
			}
			pw5.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String filename = file.getAbsolutePath().replace("\\", "/");
		loadIntoMysql(filename, "99sent");
		file.delete();
	}

	public static void loadIntoMysql(String filename, String table) throws FileNotFoundException {

		File file = new File(filename);
		long size = file.length();
		// System.out.println("loadFileIntoMysql fileSize:: " + size);
		String query = "SET GLOBAL local_infile =1;\r\n"
				+ "LOAD DATA INFILE '" + filename
				+ "' ignore INTO TABLE " + table
				+ " FIELDS TERMINATED BY '||' lines terminated by '\\n';";
//		 System.out.println("mysql query::" + query);
		try {
			if (size > 0)
				MysqlConnUtils.executeQuery(query);
			if (!table.contains("99text")) {
//				file.delete();
			}
			if (size <= 0) {
//				file.delete();
			}

		} catch (SQLException e) {
			e.printStackTrace(System.out);
		}
	}

	public static List<Integer> getClosestIndicesFromFirstList(
			List<Integer> listIdxs1, List<Integer> listIdxs2, int maxDistance) {
		// each location of 1st list is checked against the 2nd. this can return
		// list (multiple instances that satisfy the proximity rule).
		List<Integer> result = new ArrayList<Integer>();

		if (listIdxs1 != null && listIdxs2 != null) {
			for (Integer idx1 : listIdxs1) {
				for (Integer idx2 : listIdxs2) {
					if (Math.abs(idx2 - idx1) < maxDistance) {
						result.add(idx1);
						break;
						// since this idx1 is selected now, go to next idx1
					}
				}
			}
			return result;
		}
		return result;
	}

	public static void main(String[] arg) throws MalformedURLException,
			IOException, SAXException, ParseException {
		//see 0001005477-97-000100 and 0000950130-97-000341
		String acc = "div";
		String text = Utils
				 .readTextFromFile("c:/temp/"+acc+".txt");
		
//				 text = DivParser.getTextSimple(text,"8k");
//				 System.out.println("text="+text);
				 getTextBlock(text, "1997-01-29", acc,"8k");


	}
}
