package xbrl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {
	
	public static String getMontIntegerAsString(String month){
		String mo = "";
		if(month.toLowerCase().contains("jan"))
			mo="01";
		else if(month.toLowerCase().contains("feb"))
			mo="02";
		else if(month.toLowerCase().contains("mar"))
			mo="03";
		else if(month.toLowerCase().contains("apr"))
			mo="04";
		else if(month.toLowerCase().contains("may"))
			mo="05";
		else if(month.toLowerCase().contains("jun"))
			mo="06";
		else if(month.toLowerCase().contains("jul"))
			mo="07";
		else if(month.toLowerCase().contains("aug"))
			mo="08";
		else if(month.toLowerCase().contains("sep"))
			mo="09";
		else if(month.toLowerCase().contains("oct"))
			mo="10";
		else if(month.toLowerCase().contains("nov"))
			mo="11";
		else if(month.toLowerCase().contains("dec"))
			mo="12";
		
		return mo;
	}

	
	public static String format(Date date, String format) {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}
	
	public static Calendar parseToDate(String format, String dateStr) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		//format could equal e.g.,: yyyyMMdd
		Date startDt;
		startDt = sdf.parse(dateStr);
		Calendar cal = Calendar.getInstance();
		cal.setTime(startDt);
		return cal;
	}
	
		
	public static void main(String[] args) {

	}

}
