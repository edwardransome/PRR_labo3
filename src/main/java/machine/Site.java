package machine;

import util.Constantes;
import util.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class Site {
    //Identifiant du site
    private int id;
    //Identifiant de l'élu
    private int elu;

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

        electionManager = new Thread(new Runnable(){

            public void run() {
                while(true){ //Boucle principale de réception de message
                    byte[] tampon = new byte[Constantes.TAILLE_TAMPON];
                    DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);

                    try {
                        socket.receive(paquet);
                    } catch (IOException e) {
                        System.err.println("Erreur de reception de paquet");
                        e.printStackTrace();
                    }

                    if(paquet.getData()[0] == MessageType.ANNONCE.ordinal()){
                        traiterAnnonce(paquet);
                    }else if (paquet.getData()[0] == MessageType.RESULTAT.ordinal()){
                        traiterResultat(paquet);
                    }
                }
            }
        });

    }

    private void traiterAnnonce(DatagramPacket paquet) {
        ByteBuffer donneesEntieres = ByteBuffer.wrap(paquet.getData());
        int type = donneesEntieres.get();
        int[] aptitudes = new int[Constantes.NOMBRE_DE_SITES];
        for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
            aptitudes[i] = donneesEntieres.getInt();
        }

        if(aptitudes[id] != -1){
            int maximum = 0;
            for(int i = 0; i < aptitudes.length; ++i){
                if(aptitudes[i] >= aptitudes[maximum]){
                    maximum = i;
                }else if (aptitudes[i] == aptitudes[maximum]){
                    //TODO traiter égalité avec formule de la donnée
                }
            }
            elu = maximum;
            //TODO envoyer resultat(elu, liste avec mon id)
            estEnElection = false;
        }else{
            //TODO envoyer annonce
            estEnElection = true;
        }
    }

    private void traiterResultat(DatagramPacket paquet) {
        ByteBuffer donneesEntieres = ByteBuffer.wrap(paquet.getData());
        int type = donneesEntieres.get();
        int eluPaquet = donneesEntieres.getInt();
        boolean[] dansListe = new boolean[Constantes.NOMBRE_DE_SITES];
        for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
            dansListe[i] = donneesEntieres.getInt() == 1;
        }
        if(dansListe[id]){
            //TODO fin
            return;
        }else if (!estEnElection && elu != id){

        } else if (estEnElection){
            elu = eluPaquet;
            //TODO envoie
            estEnElection = false;
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
