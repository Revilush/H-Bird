package solrExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.postingshighlight.DefaultPassageFormatter;
import org.apache.lucene.search.postingshighlight.Passage;
import org.apache.lucene.search.postingshighlight.PassageFormatter;
import org.apache.lucene.search.postingshighlight.PostingsHighlighter;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.highlight.PostingsSolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;


public class CustomPostingsSolrHighlighter extends PostingsSolrHighlighter {

	
	@Override
	public NamedList<Object> doHighlighting(DocList docs, Query query,
			SolrQueryRequest req, String[] defaultFields) throws IOException {
		NamedList<Object> list = super.doHighlighting(docs, query, req,
				defaultFields);
		System.out.println(">>>>> Custom PostingsHL.doHighlighting():"+list);
		return list;
	}

	protected PostingsHighlighter getHighlighter(SolrQueryRequest req) {
		return new customSolrExtendedPostingsHighlighter(req);
	}
	
	
	

	
	public class customSolrExtendedPostingsHighlighter extends PostingsSolrHighlighter.SolrExtendedPostingsHighlighter {
		public customSolrExtendedPostingsHighlighter(SolrQueryRequest req) {
			super(req);
		}
		
		@Override
	    protected PassageFormatter getFormatter(String fieldName) {
			String preTag = params.getFieldParam(fieldName, HighlightParams.TAG_PRE, "<em>");
		      String postTag = params.getFieldParam(fieldName, HighlightParams.TAG_POST, "</em>");
		      String ellipsis = params.getFieldParam(fieldName, HighlightParams.TAG_ELLIPSIS, "... ");
		      String encoder = params.getFieldParam(fieldName, HighlightParams.ENCODER, "simple");
		      return new CustomPassageFormatter(preTag, postTag, ellipsis, "html".equals(encoder));
		}
		
		/*public Map<String,String[]> highlightFields(String[] fieldNames, Query query, SolrIndexSearcher searcher, int[] docIDs, int[] maxPassages) throws IOException {
			Map<String,String[]> snippets = super.highlightFields(fieldNames, query, searcher, docIDs, maxPassages);
			System.out.println("custom highlightFields: "+snippets);
			return snippets;
		}*/
		
	}
	
	
	public class CustomPassageFormatter extends DefaultPassageFormatter {
		public CustomPassageFormatter() {
			super();
		}

		public CustomPassageFormatter(String preTag, String postTag,
				String ellipsis, boolean escape) {
			super(preTag, postTag, ellipsis, escape);
		}

		@Override
		public String format(Passage passages[], String content) {
			StringBuilder sb = new StringBuilder();
			int pos = 0;
			double psgTtlScore = 0, maxScore=0, score;
			int psgCounts = 0;
			List<CustomPsg> psgGroups = new ArrayList<CustomPsg>();
			for (Passage passage : passages) {
				// don't add ellipsis if it's the first one, or if it's
				// connected.
				if (passage.getStartOffset() > pos && pos > 0) {
					score = psgTtlScore / psgCounts;
					if (score > maxScore)
						maxScore = score;
					sb.append("[[").append(score).append("]]");
					psgGroups.add(new CustomPsg(sb.toString(), score));
					//sb.append(ellipsis);
					psgTtlScore = 0;
					psgCounts = 0;
					sb = new StringBuilder();
				}
				psgTtlScore += passage.getScore();
				psgCounts++;
				pos = passage.getStartOffset();
				for (int i = 0; i < passage.getNumMatches(); i++) {
					int start = passage.getMatchStarts()[i];
					int end = passage.getMatchEnds()[i];
					// it's possible to have overlapping terms
					if (start > pos) {
						append(sb, content, pos, start);
					}
					if (end > pos) {
						sb.append(preTag);
						append(sb, content, Math.max(pos, start), end);
						sb.append(postTag);
						pos = end;
					}
				}
				sb.append("[").append(passage.getScore()).append("]");
				// it's possible a "term" from the analyzer could span a
				// sentence boundary.
				append(sb, content, pos, Math.max(pos, passage.getEndOffset()));
				pos = passage.getEndOffset();
			}
			sb.append("[[").append(psgTtlScore / psgCounts).append("]]");
			psgGroups.add(new CustomPsg(sb.toString(), psgTtlScore / psgCounts));
			
			
			sb = new StringBuilder();
			for (CustomPsg psg : psgGroups) {
				sb.append(psg.psg).append("{{").append(psg.score/maxScore).append("}}").append(ellipsis);
			}
			return sb.toString();
		}
	
		private class CustomPsg {
			public String psg;
			public double score;
			public CustomPsg(String psg, double score) {
				this.psg = psg;
				this.score = score;
			}
		}
	}
	
}
