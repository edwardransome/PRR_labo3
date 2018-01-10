package machine;

import javafx.scene.chart.PieChart;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Site {
    //Identifiant du site
    private int id;

    //Addresse et port du site
    private InetAddress address;
    private int port;
    private DatagramSocket socket;

    //Booléen retournant vrai si élection en cours
    private boolean inElection;

    //Thread gérant l'élection
    private Thread electionManager;
    //Thread applicatif
    private Thread application;

    public Site(int id, int port) {
        this.id = id;
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Erreur lors de l'initialisation du socket du site " + id + ".");
            e.printStackTrace();
        }

        electionManager = new Thread() {
            while(true){ //Boucle principale
                byte[] buffer = new byte[255]; //TODO constante
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);
            }
        }
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
