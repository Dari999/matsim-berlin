package org.matsim.run;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import ch.ethz.matsim.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraint;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import ch.ethz.matsim.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

/**
 * This contraint forbids "pt" trips that only consist of walk legs, i.e. there
 * is no "pt" leg included.
 * 
 * @author sebhoerl
 */
public class OnlyFallbackWalkConstraint extends AbstractTripConstraint {
	@Override
	public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate,
			List<TripCandidate> previousCandidates) {
		// keep walk if routing mode was walk
		if (!candidate.getMode().equals(TransportMode.walk)) {
			if (candidate instanceof RoutedTripCandidate) {
				// Go through all plan elments
				for (PlanElement element : ((RoutedTripCandidate) candidate).getRoutedPlanElements()) {
					if (element instanceof Leg) {
						if (((Leg) element).getMode().equals(candidate.getMode())) {
							// If we find at least one leg of the routing mode, we're good
							// -> will not work with upper level routing modes which do not exist as leg mode (e.g. PtRoutingModeWrapper)!
							return true;
						}
					}
				}

				// If there was no leg of the routing mode, we do not accept this candidate
				return false;
			} else {
				throw new IllegalStateException("Need a route to evaluate constraint");
			}
		}

		return true;
	}

	static public class Factory implements TripConstraintFactory {
		@Override
		public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> trips,
				Collection<String> availableModes) {
			return new OnlyFallbackWalkConstraint();
		}
	}
}