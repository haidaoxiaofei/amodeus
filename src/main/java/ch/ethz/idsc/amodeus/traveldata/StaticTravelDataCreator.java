/* amodeus - Copyright (c) 2018, ETH Zurich, Institute for Dynamic Systems and Control */
package ch.ethz.idsc.amodeus.traveldata;

import java.io.File;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;

import ch.ethz.idsc.amodeus.lp.LPPreparer;
import ch.ethz.idsc.amodeus.lp.LPSolver;
import ch.ethz.idsc.amodeus.virtualnetwork.core.VirtualNetwork;
import ch.ethz.idsc.tensor.Tensor;

public enum StaticTravelDataCreator {
    ;
    /** Creates the travel data by counting all travel requests and solving an LP depending on this request information */
    public static StaticTravelData create(File workingDir, VirtualNetwork<Link> virtualNetwork, Network network, //
            Population population, int interval, int numVehicles, int endTime) throws Exception {
        Tensor lambdaAbsolute = LambdaAbsolute.get(network, virtualNetwork, population, interval, endTime);
        LPSolver lpSolver = LPPreparer.run(workingDir, virtualNetwork, network, lambdaAbsolute, numVehicles, endTime);
        String lpName = lpSolver.getClass().getSimpleName();
        Tensor alphaAbsolute = lpSolver.getAlphaAbsolute_ij();
        Tensor v0_i = lpSolver.getV0_i();
        Tensor fAbsolute = lpSolver.getFAbsolute_ij();
        return new StaticTravelData(virtualNetwork.getvNetworkID(), lambdaAbsolute, alphaAbsolute, fAbsolute, v0_i, lpName, endTime);
    }

}