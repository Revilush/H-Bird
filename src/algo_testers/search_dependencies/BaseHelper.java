package algo_testers.search_dependencies;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.common.SolrDocument;

import algo_testers.search_dependencies.AppConfig.SearchAppConfig;


public abstract class BaseHelper {
	
	protected static Log log = LogFactory.getLog(BaseHelper.class);

	public static final String SolrFieldName_Score = "score";
	public static final String SolrFieldName_SimilarityScore = "sim_score";
	public static final String SolrFieldName_SolrScore = "solr_score";

	public static String solrField_txtCnt = "txtCnt";
	public static String solrField_hTxtCnt = "hTxtCnt";
	public static String solrField_hashHtxtId = "hashHtxtId";
	public static String solrField_hashTxtId = "hashTxtId";
	
	public static String solrField_hdgOrd = "hdgOrd"; 
	public static String solrField_hashHdgId = "hashHdgId";
	
	protected SearchAppConfig searchConfigContainer;

	protected boolean enabledAuditingWriteToFiles;
	protected String folderForAuditFiles;

	// 'friendly' constructor, initializing itself.
	BaseHelper(SearchAppConfig searchConfigContainer) {
		this.searchConfigContainer = searchConfigContainer;
		setEnabledAuditingWriteToFiles(searchConfigContainer.isEnableAuditingWriteToFiles());
		setFolderForAuditFiles(searchConfigContainer.getFolderForAuditFiles());
	}
	
	public boolean isEnabledAuditingWriteToFiles() {
		return enabledAuditingWriteToFiles;
	}
	public void setEnabledAuditingWriteToFiles(boolean enableAuditingWriteToFiles) {
		this.enabledAuditingWriteToFiles = enableAuditingWriteToFiles;
	}
	public String getFolderForAuditFiles() {
		return folderForAuditFiles;
	}
	public void setFolderForAuditFiles(String folderForAuditFiles) {
		this.folderForAuditFiles = folderForAuditFiles;
	}
	
	
	// **************************

	protected void writeAuditInfo(String fileName, Object... texts) {
		if (! isEnabledAuditingWriteToFiles()  ||  StringUtils.isBlank(folderForAuditFiles)  ||  StringUtils.isBlank(fileName))
			return;
		writeTextsToFile(new File(folderForAuditFiles, fileName), texts);
	}
	
	protected void writeTextsToFile(File file, Object... texts) {
		if (! file.getParentFile().exists())
			file.getParentFile().mkdirs();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for (Object txt : texts) {
				if (txt instanceof String)
					pw.append(txt.toString());
				else {
					try {
						pw.append(JSonUtils.prettyPrint(txt));
					} catch (Exception e) {
						pw.append("Error while converting object to json:" + e.getMessage());
						pw.append(txt.toString());
					}
				}
			}
		} catch (FileNotFoundException e) {
			log.warn("", e);
		} finally {
			if (null != pw)
				pw.close();
		}
	}
	
	protected static String getKId(SolrDocument doc) {
		String id = SolrUtils.getSolrDocFieldAsString(doc, "id");			//id: 0001193125-07-249519_10_76_467_1325
		String[] parts = id.split("_");
		id = parts[0]+"_"+parts[1];
		return id;
	}

	protected static float getDocScore(SolrDocument sd) {
		return SolrUtils.getSolrDocFieldAsFloat(sd, SolrFieldName_Score);
	}
	

}
