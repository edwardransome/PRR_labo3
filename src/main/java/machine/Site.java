package machine;

import util.Constantes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class Site {
    //Identifiant du site
    private int id;
    //Identifiant de l'élu
    private int elu;

    //Adresse et port du site
    private InetAddress adresse;
    private int port;
    private DatagramSocket socket;

    //Booléen retournant vrai si on se trouve dans la phase d'élection et false si on se trouve dans la phase de résultat
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

        electionManager = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) { //Boucle principale de réception de message
                    byte[] tampon = new byte[Constantes.TAILLE_TAMPON];
                    DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);

                    try {
                        socket.receive(paquet);
                    } catch (IOException e) {
                        System.err.println("Erreur de reception de paquet");
                        e.printStackTrace();
                    }

                    if (paquet.getData()[0] == Constantes.ANNONCE) {
                        Site.this.traiterAnnonce(paquet);
                    } else if (paquet.getData()[0] == Constantes.RESULTAT) {
                        Site.this.traiterResultat(paquet);
                    } else {
                        throw new IllegalArgumentException("Le type du paquet reçu n'est pas reconnu");
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
            int eluCourrant = 0;
            for(int i = 0; i < aptitudes.length; ++i){
                if(aptitudes[i] >= aptitudes[eluCourrant]){
                    eluCourrant = i;

                // S'il y a une égalité et que l'adresse ip de i est plus petite, alors i devient l'elu    //
                }else if (aptitudes[i] == aptitudes[eluCourrant] && (Constantes.ADRESSES_IP[i].compareTo(Constantes.ADRESSES_IP[eluCourrant]) < 0)){
                    eluCourrant = i ;
                }
            }
            elu = eluCourrant;
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

    private void envoi(byte[] corps, InetAddress addr, int port) throws IOException {
        //On envoit la quittance au précédent
        envoiQuittance();

        //On envoit le message au suivant
        DatagramSocket envoiSocket = new DatagramSocket();
        envoiSocket.send(new DatagramPacket(corps, corps.length,
                addr, port));

        //On attends la quittance du suivant avec timeout
        byte[] tampon = new byte[]


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
