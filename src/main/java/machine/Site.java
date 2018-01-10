package machine;

import javafx.scene.chart.PieChart;

import java.net.DatagramSocket;
import java.net.InetAddress;

public class Site {
    //Identifiant du site
    private int id;

    //Addresse et port du site
    private InetAddress address;
    private int port;

    //Booléen retournant vrai si élection en cours
    private boolean inElection;

    //Thread gérant l'élection
    private Thread electionManager;
    //Thread applicatif
    private Thread application;

    public Site(int id, int port) {
        this.id = id;
        socket = new DatagramSocket(port);
    }

    /**
     * Méthode utilitaire qui retourne l'aptitude d'un site
     * @param adr
     * @param port
     * @return
     */
    public static int getApititude(InetAddress adr, int port){
        return port + adr.getAddress()[3];
    }

    /**
     * Méthode utilitaire qui détermine la plus grande aptitude parmi deux sites
     * @param firstAdr
     * @param firstPort
     * @param secondAdr
     * @param secondPort
     * @return
     */
    public static boolean aptitudeHigherThan(InetAddress firstAdr, int firstPort,
                                             InetAddress secondAdr, int secondPort){
        return getApititude(firstAdr, firstPort) > getApititude(secondAdr, secondPort)
                ? true
                : firstAdr.getAddress()[3] < secondAdr.getAddress()[3];
    }

}
