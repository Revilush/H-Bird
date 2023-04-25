package contracts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import charting.JSonUtils;


public class Synonyms {

	
	
	public static List<String[]> getListOfSynonyms_Old() {
		// first word is the synonyms to be used the rest are replaced
		List<String[]> listSynonyms = new ArrayList<>();

		// don't fix bad drafting - that's the stuff we want to fix!
		// syns must replace perfectly English works 100% - otherwise don't use it! Or
		// it will screw up the redline and make the reader believe the program doesn't
		// work. Multi-word synonyms are hazardous for this reason

		String[] agentAty = { "agents or attorneys", "attorneys? or agents?" };
		listSynonyms.add(agentAty);

		String[] companyIssuer = { "Company", "Issuer" };
		listSynonyms.add(companyIssuer);

		String[] eventOfDefault = { " a Default", " an (Indenture |Event )of Default or Default",
				"( a| an) Default or (an )?Event of Default", " an Event of Default",
				"( a| an) (Enforcement |Amortization |Termination )Event",
				"( a| an) Default or (a |an )?Servicer Default",
				"( a| an) Default or (an )?Event of Servicing Termination" };
		listSynonyms.add(eventOfDefault);

		String[] occured = { " occurred", " happend" };
		listSynonyms.add(occured);

		String[] occuring = { " occuring", " happening" };
		listSynonyms.add(occuring);

		String[] person = { " person", " Person" };
		listSynonyms.add(person);

		String[] responsibleOfficer = { "Responsible Officer", "Trust Officer" };
		listSynonyms.add(responsibleOfficer);

		String[] that = { "\\(that ", "\\(which " };
		listSynonyms.add(that);

		String[] that2 = { " that ", " which " };
		listSynonyms.add(that2);

		String[] shall = { "shall", "will" };
		listSynonyms.add(shall);

//		with respect to the Securities of a series
		String[] series = { "",
				"(?i),? with respect to (a|any) (tranche|series) of "
						+ "(Debentures|Debt Securities|Notes|Securities|Bonds),?",
				"(?i),? with respect to such (tranche|series),?",
				"(?i),? with respect to the (Debentures|Debt Securities|Notes|Securities|Bonds) of a (tranche|series),?",
				"(?i),? with respect to a particular (tranche|series),?",
				"(?i),? with respect to (the )?" + "(Debentures|Debt Securities|Notes|Securities|Bonds) "
						+ "of (that |such |any )?(a particular )?(tranche|series),?" };
		listSynonyms.add(series);

//		, with respect to such Securities,
		String[] withRespectToThe = { "",
				"(?i),? with respect to (the|such) (Debentures|Debt Securities|Notes|Securities|Bonds),?" };
		listSynonyms.add(withRespectToThe);

		String[] trustee = { "Trustee", "U.S. Trustee", "Indenture Trustee" };
		listSynonyms.add(trustee);

//		Notwithstanding anything to the contrary herein
//		Notwithstanding anything to the herein contrary 
//		Notwithstanding anything to the contrary contained herein

//		In case
//		If

//Officer, Officer's, Officers',Officers

		return listSynonyms;
	}
	
	
	public static void main(String[ ] arg) throws SolrServerException, IOException {
	}
	
}
