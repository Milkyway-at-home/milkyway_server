package milkyway_server;

import fgdo_java.daemons.ValidationPolicy;

import fgdo_java.database.Result;
import fgdo_java.database.Workunit;

import fgdo_java.searches.SearchManager;
import fgdo_java.searches.SearchResult;
import fgdo_java.searches.SearchResultException;

import fgdo_java.util.XMLTemplate;
import fgdo_java.util.XMLParseException;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.text.NumberFormat;

public class MilkywayValidationPolicy implements ValidationPolicy {
	public final static double MAXIMUM_ERROR = 10e-10;

	private NumberFormat nf = NumberFormat.getInstance();
	public MilkywayValidationPolicy() {
		nf.setMinimumFractionDigits(20);
	}

	public boolean checkPair(Result canonicalResult, Result unvalidatedResult) {
		String canonical_filename = canonicalResult.getFullOutputFile();
		String unvalidated_filename = unvalidatedResult.getFullOutputFile();

		File canonical_file = new File(canonical_filename);
		System.out.println("canonical_output_file: " + canonical_filename+ " exists? " + canonical_file.exists());

		File unvalidated_file = new File(unvalidated_filename);
		System.out.println("unvalidated_output_file: " + unvalidated_filename+ " exists? " + unvalidated_file.exists());

		SearchResult canonicalSearchResult = null;
		try {
			canonicalSearchResult = new SearchResult(canonical_file);
		} catch (SearchResultException e) {
			System.err.println("Could not get canonical search result file:");
			System.err.println("Exception: " + e);
			e.printStackTrace();
			return true;
		}
		System.out.println("canonicalSearchResult: " + canonicalSearchResult);

		SearchResult unvalidatedSearchResult = null;
		try {
			unvalidatedSearchResult = new SearchResult(unvalidated_file);
		} catch (SearchResultException e) {
			System.err.println("Could not get unvalidated search result file:");
			System.err.println("Exception: " + e);
			e.printStackTrace();

			unvalidatedResult.setValidateState(Result.VALIDATE_STATE_INVALID);
			return false;
		}
		System.out.println("unvalidateSearchResult: " + unvalidatedSearchResult);

		if (Math.abs(canonicalSearchResult.getFitness() - unvalidatedSearchResult.getFitness()) < MAXIMUM_ERROR) {
			unvalidatedResult.setValidateState(Result.VALIDATE_STATE_VALID);
		} else {
			SearchManager.notifyInvalidatedIndividual( unvalidatedSearchResult );

			unvalidatedResult.setValidateState(Result.VALIDATE_STATE_INVALID);
		}

		return false;
	}

	public boolean checkSet(Workunit workunit, LinkedList<Result> unvalidatedResults) {
		LinkedList<SearchResult> searchResults = new LinkedList<SearchResult>();

		String workunit_template = workunit.getWorkunitTemplate();
		double[] parameters = null;
		String searchName = "";
		String metadata = "";

//		if (System.getProperty("generate_wu_file") == null) {
			try {
				parameters = XMLTemplate.processDoubleArray(workunit_template, "search_parameters");
				searchName = XMLTemplate.processString(workunit_template, "search_name");
				metadata = XMLTemplate.processString(workunit_template,"search_metadata");

			} catch (XMLParseException e) {
				System.err.println("could not parse workunit XML: " + e);
				e.printStackTrace();
				System.exit(0);
			}
//		}

		int k = 0;
		for (Result unvalidatedResult : unvalidatedResults) {
//			System.out.println("unvalidated_output_file[" + k + "]: " + unvalidated_filename+ " exists? " + unvalidated_file.exists());

			try {
				if (System.getProperty("generate_wu_file") == null) {
					String resultString = unvalidatedResult.getStderrOut();

					String application = XMLTemplate.processString(resultString, "search_application");
					double fitness = XMLTemplate.processDouble(resultString, "search_likelihood");

					searchResults.add(new SearchResult(searchName, application, fitness, parameters, metadata));

				} else {
					try {
						String resultString = unvalidatedResult.getStderrOut();

						String application = XMLTemplate.processString(resultString, "search_application");

						double fitness = XMLTemplate.processDouble(resultString, "search_likelihood");

						SearchResult tmp = new SearchResult(searchName, application, fitness, parameters, metadata);
						searchResults.add(tmp);

						System.out.println("!!! Got result from XML: " + tmp);

					} catch (XMLParseException e) {
						String unvalidated_filename = unvalidatedResult.getFullOutputFile();
						File unvalidated_file = new File(unvalidated_filename);
						searchResults.add(new SearchResult(unvalidated_file));
					}
				}
			} catch (XMLParseException e) {
				System.err.println("Could not get unvalidated search result, xml parse exception.");
				System.err.println("Exception: " + e);
				e.printStackTrace();

				unvalidatedResults.get(k).setOutcome(Result.RESULT_OUTCOME_VALIDATE_ERROR);
				searchResults.add(new SearchResult(false));

			} catch (SearchResultException e) {
				System.err.println("Could not get unvalidated search result file:");
				System.err.println("Exception: " + e);
				e.printStackTrace();

				unvalidatedResults.get(k).setOutcome(Result.RESULT_OUTCOME_VALIDATE_ERROR);
				searchResults.add(new SearchResult(false));
			}
//			System.out.println("unvalidateSearchResult[" + k + "]: " + searchResults.get(k));

			k++;
		}


		if (unvalidatedResults.size() == 1) {
			if ( !searchResults.get(0).isValid() ) {
				unvalidatedResults.get(0).setValidateState(Result.VALIDATE_STATE_INVALID);
				return true;

			} else if (searchResults.get(0).getFitness() > -1.0) {
				System.out.println("Bad Fitness: " + searchResults.get(0));

				unvalidatedResults.get(0).setValidateState(Result.VALIDATE_STATE_INVALID);
				return true;

			} else if (searchResults.get(0).getApplication() != null && searchResults.get(0).getApplication().contains("osx")) {
				unvalidatedResults.get(0).setValidateState(Result.VALIDATE_STATE_VALID);
				workunit.setCanonicalResultId(unvalidatedResults.get(0).getId());
				return false;
			}

			if (workunit.getMinQuorum() == 1) {
				if ( !SearchManager.requiresValidation(searchResults.get(0)) && Math.random() > 0.5 ) {
					unvalidatedResults.get(0).setValidateState(Result.VALIDATE_STATE_VALID);
					workunit.setCanonicalResultId(unvalidatedResults.get(0).getId());
					return false;
				} else {
					workunit.setMinQuorum(2);
				}
			}
		}

		double abs_diff = 0;
		double min_difference = Double.MAX_VALUE;
		double max_difference = 0;
		Result canonicalResult = null;
		double canonicalFitness = 0;
		double fitness1, fitness2;
		int matches = 0;
		for (int i = 0; i < searchResults.size() && canonicalResult == null; i++) {
			if (searchResults.get(i).getApplication() != null && searchResults.get(i).getApplication().contains("osx")) continue;
			if (!searchResults.get(i).isValid()) continue;
			fitness1 = searchResults.get(i).getFitness();
			matches = 0;

			for (int j = 0; j < searchResults.size() && canonicalResult == null; j++) {
				if (j == i) continue;
				if (searchResults.get(i).getApplication() != null && searchResults.get(i).getApplication().contains("osx")) continue;
				if (!searchResults.get(j).isValid()) continue;
				fitness2 = searchResults.get(j).getFitness();

				abs_diff = Math.abs(fitness1 - fitness2);
				if (abs_diff > max_difference) max_difference = abs_diff;
				if (abs_diff < min_difference) min_difference = abs_diff;

				if (abs_diff < MAXIMUM_ERROR) {
					matches++;

					if (matches >= (workunit.getMinQuorum() - 1)) {
						canonicalFitness = fitness1;
						canonicalResult = unvalidatedResults.get(i);
						workunit.setCanonicalResultId(canonicalResult.getId());
					}
				}
			}
		}
		if (searchResults.size() > 1) {
			System.out.println("Examined " + searchResults.size() + " of " + unvalidatedResults.size() + " results, minimum difference: " + min_difference + ", maximum difference: " + max_difference + ", canonical result? " + (canonicalResult != null));
			System.out.print("\tApplications:");
			for (SearchResult result : searchResults) {
				System.out.print(" [" + result.getApplication() + "]");
			}
			System.out.println();
		}

		//we didnt find a canonical result
		if (canonicalResult == null) {
			SearchManager.notifyUnvalidatedIndividual( Collections.max(searchResults) );
			return false;
		}

		LinkedList<SearchResult> validResults = new LinkedList<SearchResult>();
		for (SearchResult searchResult : searchResults) {
			if (searchResult.isValid() && Math.abs(canonicalFitness - searchResult.getFitness()) < MAXIMUM_ERROR) {
				validResults.add(searchResult);
			}
		}
		SearchManager.notifyValidatedIndividual( Collections.max(validResults) );

		for (int i = 0; i < searchResults.size(); i++) {
			if (searchResults.get(i).getApplication() != null && searchResults.get(i).getApplication().contains("osx")) {
				unvalidatedResults.get(i).setValidateState(Result.VALIDATE_STATE_VALID);
			} else {

				if (searchResults.get(i).isValid() && Math.abs(canonicalFitness - searchResults.get(i).getFitness()) < MAXIMUM_ERROR) {
					unvalidatedResults.get(i).setValidateState(Result.VALIDATE_STATE_VALID);
				} else {
					SearchManager.notifyInvalidatedIndividual( searchResults.get(i) );

					unvalidatedResults.get(i).setValidateState(Result.VALIDATE_STATE_INVALID);
				}

			}
		}

		return false;
	}

}
