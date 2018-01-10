package machine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Site {
    //Identifiant du site
    private int id;

    //Adresse et port du site
    private InetAddress adresse;
    private int port;
    private DatagramSocket socket;

    //Booléen retournant vrai si élection en cours
    private boolean estEnElection;

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
                byte[] tampon = new byte[255]; //TODO constante
                DatagramPacket packet = new DatagramPacket(tampon, tampon.length);

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
    public static int getAptitude(InetAddress adr, int port){
        return port + adr.getAddress()[3];
    }

    /**
     * Méthode utilitaire qui détermine la plus grande aptitude parmi deux sites
     * @param adresseUn
     * @param portUn
     * @param adresseDeux
     * @param portDeux
     * @return
     */
    public static boolean aptitudeHigherThan(InetAddress adresseUn, int portUn,
                                             InetAddress adresseDeux, int portDeux){
        return getAptitude(adresseUn, portUn) > getAptitude(adresseDeux, portDeux)
                ? true
                : adresseUn.getAddress()[3] < adresseDeux.getAddress()[3];
    }

}
