package algo_testers.search_dependencies;

import java.util.Comparator;

import org.apache.solr.common.SolrDocument;

public abstract class SolrDocumentFieldComparator implements Comparator<SolrDocument> {
	protected String fieldName = "";
	protected int order = 1;
	
	protected SolrDocumentFieldComparator(String fieldName) {
		this.fieldName = fieldName;
	}
	/**
	 * fieldname, and order(1 = ASC, -1 = DESC)
	 * @param fieldName
	 * @param order
	 */
	protected SolrDocumentFieldComparator(String fieldName, int order) {
		this.fieldName = fieldName;
		this.order = order;
	}
	
	
	
	public static class SolrDocumentIntFieldComparator extends SolrDocumentFieldComparator {
		public SolrDocumentIntFieldComparator(String fieldName) {
			super(fieldName);
		}
		public SolrDocumentIntFieldComparator(String fieldName, int order) {
			super(fieldName, order);
		}
		@Override
		public int compare(SolrDocument o1, SolrDocument o2) {
			Integer dc1 = SolrUtils.getSolrDocFieldAsInteger(o1, fieldName);
			Integer dc2 = SolrUtils.getSolrDocFieldAsInteger(o2, fieldName);
			return dc1.compareTo(dc2) * order;
		}
	}
	
	
	public static class SolrDocumentDoubleFieldComparator extends SolrDocumentFieldComparator {
		public SolrDocumentDoubleFieldComparator(String fieldName) {
			super(fieldName);
		}
		public SolrDocumentDoubleFieldComparator(String fieldName, int order) {
			super(fieldName, order);
		}
		@Override
		public int compare(SolrDocument o1, SolrDocument o2) {
			Double dc1 = SolrUtils.getSolrDocFieldAsDouble(o1, fieldName);
			Double dc2 = SolrUtils.getSolrDocFieldAsDouble(o2, fieldName);
			return dc1.compareTo(dc2) * order;
		}
	}
	
	
}
