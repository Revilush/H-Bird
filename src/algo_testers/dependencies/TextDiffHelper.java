package algo_testers.dependencies;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import xbrl.NLP;

public abstract class TextDiffHelper {
	
	protected static final Log log = LogFactory.getLog(TextDiffHelper.class);

	public abstract List<DiffDetail> getDiff(String text1, String text2);
	public abstract List<DiffDetail> convertNativeDiffList(List<? extends Object> diffs);
	
	public int getDiffWordsCount(List<DiffDetail> diffs) {
		int c = 0;
		for (DiffDetail dif: diffs) {
			if (dif.op == DiffOperation.EQ)
				continue;
			c += dif.text.trim().split(" ").length;
		}
		return c;
	}
	public int getDiffLettersCount(List<DiffDetail> diffs) {
		int c = 0;
		for (DiffDetail dif: diffs) {
			if (dif.op == DiffOperation.EQ)
				continue;
			c += dif.text.length();
		}
		return c;
	}
	
	public String getDiffHtml(List<DiffDetail> diffs) {
		StringBuilder sb = new StringBuilder("<span>").append(getDiffText(diffs.get(0)));
		DiffDetail dif;
		for (int i=1; i < diffs.size(); i++) {
			dif = diffs.get(i);
			sb.append(getDiffText(dif));
		}
		sb.append("</span>");
		return sb.toString();
	}
	
	
	public String getDiffDeletionHtml(String text) {
		//<span class="wikEdDiffDelete" title="-">word1<span class="wikEdDiffSpace"><span class="wikEdDiffSpaceSymbol"></span></span>word2.......</span>
		String[] words = text.split(" ");
		String html = "<span class='wikEdDiffDelete' title='-'>" + StringUtils.join(words, this.getDiffSpaceHtml()) + "</span>";
		return html;
	}
	public String getDiffInsertionHtml(String text){
		//<span class="wikEdDiffInsert" title="+">word1<span class="wikEdDiffSpace"><span class="wikEdDiffSpaceSymbol"></span></span>word2......</span>
		String[] words = text.split(" ");
		String html = "<span class='wikEdDiffInsert' title='+'>" + StringUtils.join(words, this.getDiffSpaceHtml()) + "</span>";
		return html;
	}
	
	public String getCSS() {
		String css = ".wikEdDiffInsert , .wikEdDiffInsertBlank {" +
			"text-decoration:underline;" +
			"color: #E11; border-radius: 0.25em; " +
		"} " +
		// Delete
		".wikEdDiffDelete , .wikEdDiffDeleteBlank{" +
			"color: #F00 !important; text-decoration:line-through !important; " +
			"color: #222; border-radius: 0.25em; " +
		"} "+
		".wikEdDiffSpace { position: relative; } " +
		".wikEdDiffSpaceSymbol { position: absolute; top: -0.2em; left: -0.05em; } " +
		".wikEdDiffSpaceSymbol:before { content: ''; color: transparent; } "
		;
		
		return css;
	}
	
	/**
	 * Returns the final resulting plain text by removing the deletions, adding the insertions and keeping the equals.
	 * @param diffs
	 * @return
	 */
	public String getFinalPlainText(List<DiffDetail> diffs) {
		StringBuilder sb = new StringBuilder();
		for (DiffDetail dd : diffs) {
			// except DEL, keep all texts
			if (dd.op != DiffOperation.DEL)
				sb.append(dd.text);
		}
		return sb.toString();
	}
	
	
	// ******************************************* //
	
	private String getDiffText(DiffDetail dif) {
		if (dif.op == DiffOperation.DEL)
			return getDiffDeletionHtml(dif.text);
		else if (dif.op == DiffOperation.INS)
			return getDiffInsertionHtml(dif.text);
		else 
			return dif.text;
	}
	private String getDiffSpaceHtml() {
		return "<span class='wikEdDiffSpace'><span class='wikEdDiffSpaceSymbol'></span> </span>";
	}
	
	// ******************************************* //
	// ******************************************* //
	
	@JsonInclude(Include.NON_NULL)
	public static class DiffDetail {
		public DiffOperation op = null;
		public String text = null;
		public Integer seq = null;
		public Integer index = null;
		
		public DiffDetail(String op, String text) {
			this(op, text, null);
		}
		public DiffDetail(DiffOperation op, String text) {
			this(op, text, null);
		}
		public DiffDetail(String op, String text, Integer seq) {
			this(DiffOperation.getBySymbolWordOrName(op), text, seq, null);
		}
		public DiffDetail(DiffOperation op, String text, Integer seq) {
			this(op, text, seq, null);
		}
		public DiffDetail(DiffOperation op, String text, Integer seq, Integer index) {
			this.op = op;
			this.text = text;
			this.seq = seq;
			this.index = index;
		}
		
		@Override
		public String toString() {
			return toJson();
		}
		public String toJson() {
			StringBuilder sb = new StringBuilder("{");
			sb.append("\"op\":\"").append(op).append("\"");
			sb.append(",\"text\":\"").append(text).append("\"");
			if (null != seq)
				sb.append(",\"seq\":\"").append(seq).append("\"");
			if (null != index)
				sb.append(",\"index\":\"").append(index).append("\"");
			sb.append("}");
			return sb.toString();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof DiffDetail))
				return false;
			DiffDetail dd = (DiffDetail)obj;
			if (dd.op != this.op)
				return false;
			if (!StringUtils.equals(dd.text, this.text))
				return false;
			if (dd.seq != this.seq)
				return false;
			return true;
		}
	}
	
	
	// ******************************************* //
	
	public static enum DiffOperation {
		EQ ("=", "EQUAL")
		, DEL ("-", "DELETE")
		, INS ("+", "INSERT")
		;
		
		
		private String symbol;
		private String fullName;
		
		DiffOperation(String symbol, String fullName) {
			this.symbol = symbol;
			this.fullName = fullName;
		}
		
		public String getSymbol() {
			return symbol;
		}
		public String getFullName() {
			return fullName;
		}

		/**
		 * Returns the DiffOperation instance by word or symbol: 
		 * 		+/INS/INSERT	> returns 'INS' 
		 * 		-/DEL/DELETE	> returns 'DEL' 
		 * 		=/EQ/EQUAL		> returns 'EQ' 
		 * @param symbolWordOrName
		 * @return
		 */
		public static DiffOperation getBySymbolWordOrName(String symbolWordOrName) {
			String swn = new String(symbolWordOrName.toUpperCase());
			if (StringUtils.equalsAny(swn, INS.symbol, INS.name(), INS.fullName))
				return INS;
			if (StringUtils.equalsAny(swn, DEL.symbol, DEL.name(), DEL.fullName))
				return DEL;
			if (StringUtils.equalsAny(swn, EQ.symbol, EQ.name(), EQ.fullName))
				return EQ;
			return null;
		}
		
	}
	
	
	protected List<String> getAllMatchedGroups(String text, Pattern pattern) {
		try {
			return new NLP().getAllMatchedGroups(text, pattern);
		} catch (IOException e) {
			return null;
		}
	}
	
}
