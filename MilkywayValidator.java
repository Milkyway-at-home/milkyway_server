package milkyway_server;

import fgdo_java.daemons.Validator;
import fgdo_java.util.DirectoryTree;

public class MilkywayValidator {
	public static void main(String[] arguments) {
		DirectoryTree.setBaseDirectory("/export/www/boinc/milkyway/");
		DirectoryTree.setBaseURL("http://milkyway.cs.rpi.edu/milkyway/");
		DirectoryTree.setResultsDirectory("/export/www/boinc/milkyway/results/milkyway/");

		Validator validator = new Validator(1, 0, Integer.parseInt(arguments[0]), new MilkywayCreditPolicy(), new MilkywayValidationPolicy());
		validator.start();
	}
}
