package milkyway_server;

import java.io.File;
import java.io.IOException;

import fgdo_java.searches.DifferentialEvolution;
import fgdo_java.searches.Search;
import fgdo_java.searches.SearchManager;
import fgdo_java.util.DirectoryTree;
import fgdo_java.util.WorkunitInformation;

import java.util.Arrays;


public class Milkyway3StartSearch {

	private static int toGenerate = 2000;

	private static String workunit_xml_filename = "templates/milkyway3_wu.xml";
	private static String result_xml_filename = "templates/milkyway3_result.xml";

	public static void main(String[] arguments) {
		String searchName = arguments[0];
		File astronomyParametersFile = new File(arguments[1]);
		File starFile = new File(arguments[2]);

		DirectoryTree.setBaseDirectory("/export/www/boinc/milkyway/");
		DirectoryTree.setBaseURL("http://milkyway.cs.rpi.edu/milkyway/");
		DirectoryTree.setResultsDirectory("/export/www/boinc/milkyway/results/milkyway/");

		Stars sp = Stars.readFromFile( starFile );
		AstronomyParameters ap = AstronomyParameters.readFromFile( astronomyParametersFile );

		if (sp == null || ap == null) return;

		double fpops = 0;
		double fpops_new = 0;

		System.out.println("looping over number intergrals: " + ap.getNumberIntegrals());
		for (Integral integral : ap.getIntegrals()) { 
			fpops += (integral.getRSteps()/100.0) * (integral.getMuSteps()/100.0) * (integral.getNuSteps()/100.0);
			fpops_new += (integral.getMuSteps()/100.0) * (integral.getRSteps()/100.0) * (5.0 + integral.getNuSteps() * (7.0 + 5.0 * ap.getNumberStreams() + ap.getConvolve() * (35.0 + 52.0 * ap.getNumberStreams())));
		}
		double integral_flops = fpops * (4.0 + 2.0 * ap.getNumberStreams() + ap.getConvolve() * (56 + 58 * ap.getNumberStreams()));

		double likelihood_flops = sp.getNumberStars() * (ap.getConvolve() * (100.0 + ap.getNumberStreams() * 58.0) + 251.0 + ap.getNumberStreams() * 12.0 + 54.0);

		double flops = (integral_flops * 100.0 * 100.0 * 100.0) + likelihood_flops;
		double flops_new = (fpops_new * 100.0 * 100.0) + likelihood_flops;

		double multiplier = 5.4;
		double credit = multiplier * flops / 1000000000000.0;
		double credit_new = multiplier * flops_new / 1000000000000.0;

		System.out.println("credit old: " + credit);
		System.out.println("credit new: " + credit_new);

		double rsc_fpops_est = flops;
		double rsc_fpops_bound = flops * 100;
		double rsc_disk_bound = 15e6;

		/**
		 * Try to copy the star file and astronomy parameters file to the BOINC download directory so the users can download them.
		 */

		try {
			DirectoryTree.copyToDownloadDirectory( starFile );
		} catch (Exception e) {
			System.err.println("Could not copy files to download directory, exception thrown: " + e);
			e.printStackTrace();
			return;
		}

		try {
			DirectoryTree.copyToDownloadDirectory( astronomyParametersFile );
		} catch (Exception e) {
			System.err.println("Could not copy files to download directory, exception thrown: " + e);
			e.printStackTrace();
			return;
		}


		/**
		 * Generate the additional workunit information needed to create the workunits, and save it to a file so the assimilator/validator knows how to generate workunits correctly.
		 */

		WorkunitInformation wuInfo = new WorkunitInformation();

		wuInfo.addExtraXML("<credit>" + credit + "</credit>");
		wuInfo.addExtraXML("<rsc_fpops_est>" + rsc_fpops_est + "</rsc_fpops_est>");                                 
		wuInfo.addExtraXML("<rsc_fpops_bound>" + rsc_fpops_bound + "</rsc_fpops_bound>");
		wuInfo.addExtraXML("<rsc_disk_bound>" + rsc_disk_bound + "</rsc_disk_bound>");

		wuInfo.addInputFile(astronomyParametersFile.getName());
		wuInfo.addInputFile(starFile.getName());

		wuInfo.setWorkunitTemplateFile(workunit_xml_filename);
		wuInfo.setResultTemplateFile(result_xml_filename);

		File directory = new File(DirectoryTree.getResultsDirectory() + searchName);
		File workunitFile = new File(DirectoryTree.getResultsDirectory() + searchName + "/workunit_info");

		if (workunitFile.exists()) wuInfo = WorkunitInformation.getFromFile(workunitFile);
		else {
			try {
				if ( !directory.exists() ) directory.mkdir();

				wuInfo.writeToFile( workunitFile );
			} catch (IOException e) {
				System.err.println("Could not write workunit file: " + workunitFile);
				System.err.println("Threw exception: " + e);
				e.printStackTrace();

				return;
			}
		}

		System.out.println("number parameters: " + ap.getNumberParameters());
		System.out.println("min bound: " + Arrays.toString(ap.getMinBound()));
		System.out.println("max bound: " + Arrays.toString(ap.getMaxBound()));

		DifferentialEvolution de = new DifferentialEvolution(searchName, 300, ap.getNumberParameters(), ap.getMinBound(), ap.getMaxBound());
		try {
			de.writeToFile();
		} catch (Exception e) {
			System.err.println("Error writing search file: " + e);
			e.printStackTrace();
			return;
		}

		Search search = SearchManager.getSearch(searchName);
		SearchManager.generateWorkunits(toGenerate);
	}
}
