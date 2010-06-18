package milkyway_server;

import fgdo_java.daemons.CreditPolicy;

import fgdo_java.database.Result;
import fgdo_java.database.Workunit;

import fgdo_java.searches.SearchManager;

public class MilkywayCreditPolicy extends CreditPolicy {

	public boolean assignCredit(Workunit workunit, Result result) {
		if (workunit.getCanonicalCredit() == 0) {
			double credit = SearchManager.getCreditFor(workunit);
			workunit.setCanonicalCredit(credit);
			result.setGrantedCredit(credit);
		} else {
			result.setGrantedCredit(workunit.getCanonicalCredit());
		}

		return true;
	}

}
