package machine;

import util.Constantes;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Class représentant un site d'un système réparti. Un site possède un
 * identifiant, un thread gestionnaire et un thread applicatif. Les sites
 * communiquent entre eux via messages point-à-point UDP dans une topologie
 * en anneau.
 */
public class Site {
    //Identifiant du site
    private int id;
    //Identifiant du prochain site dans la liste
    private int idSuivant;
    //Identifiant de l'élu
    private int elu;
    //Aptitude du site
    private final int aptitude;


    private DatagramSocket socket;

    //Booléen retournant vrai si on se trouve dans la phase d'élection et false si on se trouve dans la phase de résultat
    private boolean estEnElection;

    //Thread gérant l'élection
    private Thread electionManager;
    //Thread applicatif
    private Thread application;

    public Site(int id, int port) {
        this.id = id;
        this.aptitude = getAptitude();
        this.idSuivant = (id + 1) % Constantes.NOMBRE_DE_SITES;
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
                // S'il y a une égalité et que l'adresse ip de i est plus petite, alors i devient l'elu
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
            try {
                ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_RESULTAT);
                byteBuffer.put(Constantes.RESULTAT);
                byteBuffer.putInt(elu);
                //Liste d'entier ou l'élément i vaut 1 si le site est présent dans la liste, 0 sinon
                int[] liste = new int[Constantes.NOMBRE_DE_SITES];
                liste[id] = 1;
                for(int i = 0; i < liste.length; ++i){
                    byteBuffer.putInt(liste[i]);
                }
                envoi(byteBuffer.array(), paquet);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du datagramme");
                e.printStackTrace();
            }
            estEnElection = false;
        }

    }

    private void envoi(byte[] corps, DatagramPacket paquetOriginal) throws Exception {
        //On envoie la quittance au précédent
        envoiQuittance(paquetOriginal);

        //On envoie le message au suivant
        DatagramSocket envoiSocket = new DatagramSocket();
        envoiSocket.send(new DatagramPacket(corps, corps.length,
                InetAddress.getByName(Constantes.ADRESSES_IP[idSuivant]), Constantes.PORTS[idSuivant]));

        //On attends la quittance du suivant avec timeout
        byte[] tampon = new byte[Constantes.TAILLE_TAMPON_QUITTANCE];
        DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);
        try{
            envoiSocket.setSoTimeout(Constantes.MESSAGE_TIMEOUT);
            envoiSocket.receive(paquet);
        } catch (SocketTimeoutException e){
            //La quittance n'a pas été reçue
            idSuivant = (idSuivant + 1) % Constantes.NOMBRE_DE_SITES;
            if(idSuivant == id){
                //On a fait tout le tour de l'anneau
                idSuivant = (idSuivant + 1) % Constantes.NOMBRE_DE_SITES;
            }
            //Alors envoie le message au suivant
            envoi(corps, paquetOriginal);
        }

        if(paquet.getData()[0] == Constantes.QUITTANCE){
            ByteBuffer byteBuffer = ByteBuffer.wrap(paquet.getData());
            byte type = byteBuffer.get();
            int id = byteBuffer.getInt();
            if(id != idSuivant){
                throw new Exception("Quittance reçue du mauvais site");
            }
        }
    }

    /**
     * Méthode qui envoi une quittance au site qui a envoyé le paquet en paramètre
     * @param paquetOriginal
     * @throws IOException
     */
    private void envoiQuittance(DatagramPacket paquetOriginal) throws IOException {
        DatagramSocket envoiQuittance = new DatagramSocket();
        byte[] tampon = new byte[]{Constantes.QUITTANCE, Integer.valueOf(id).byteValue()};
        envoiQuittance.send(new DatagramPacket(tampon, tampon.length, paquetOriginal.getAddress(), paquetOriginal.getPort()));
    }

    /**
     * Méthode qui retourne l'aptitude du site
     */
    public int getAptitude(){
         return Constantes.PORTS[id] + Integer.parseInt(Constantes.ADRESSES_IP[id].substring(0,Constantes.ADRESSES_IP[id].lastIndexOf('.')));
    }

}
