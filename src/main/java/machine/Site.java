package machine;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;
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
    private Thread applicatif;

    /**
     * Construit un site à partir de son identifiant. On part de l'hypothèse
     * que l'utilisateur ne créé pas deux sites du même identifiant.
     * @param id
     * @param port
     */
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

                    switch (paquet.getData()[0]) {
                        case Constantes.ANNONCE:
                            traiterAnnonce(paquet);
                            break;
                        case Constantes.RESULTAT:
                            traiterResultat(paquet);
                            break;
                        case Constantes.CHECK:
                            try {
                                envoiQuittance(paquet);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Le type du paquet reçu n'est pas reconnu");
                    }
                }
            }
        });
        electionManager.start();

        applicatif = new Thread(new Runnable() {
            @Override
            public void run() {
                final int PERIODE_CHECK = 2000;

                initialiseElection();

                while (true) {

                    int currentElu = getElu();

                    try {
                        DatagramSocket check = new DatagramSocket();
                        byte[] tampon = new byte[]{Constantes.CHECK, Integer.valueOf(id).byteValue()};
                        check.send(new DatagramPacket(tampon, tampon.length, InetAddress.getByName(Constantes.ADRESSES_IP[currentElu]), Constantes.PORTS[currentElu]));


                        //On attends la quittance du suivant avec timeout
                        byte[] tampon_quittance = new byte[Constantes.TAILLE_TAMPON_QUITTANCE];
                        DatagramPacket paquet = new DatagramPacket(tampon_quittance, tampon_quittance.length);

                        check.setSoTimeout(Constantes.MESSAGE_TIMEOUT);
                        check.receive(paquet);

                        Thread.sleep(PERIODE_CHECK);
                    } catch (java.net.SocketTimeoutException e) {
                        initialiseElection();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        applicatif.start();
    }

    /**
     * Démarre une élection en transmettant le premier message ANNONCE
     */
    private void initialiseElection(){
        estEnElection = true;

        ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_ANNONCE);
        //L'index de notre liste correspond au site, donc les sites non
        //initialisés ont une aptitude initiale de -1
        byteBuffer.put(Constantes.ANNONCE);
        for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
            byteBuffer.putInt(-1);
        }
        try {
            envoi(byteBuffer.array(), null);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du datagramme");
            e.printStackTrace();
        }
    }

    /**
     * Traite un paquet reçu de type ANNONCE selon l'algorithme d'élection en
     * anneau avec pannes.
     * @param paquet
     */
    private void traiterAnnonce(DatagramPacket paquet) {
        ByteBuffer donneesEntieres = ByteBuffer.wrap(paquet.getData());
        int type = donneesEntieres.get();
        int[] aptitudes = new int[Constantes.NOMBRE_DE_SITES];
        for (int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i) {
            aptitudes[i] = donneesEntieres.getInt();
        }

        if (aptitudes[id] != -1) {
            int eluCourrant = 0;
            for (int i = 0; i < aptitudes.length; ++i) {
                if (aptitudes[i] >= aptitudes[eluCourrant]) {
                    eluCourrant = i;
                    // S'il y a une égalité et que l'adresse ip de i est plus petite, alors i devient l'elu
                } else if (aptitudes[i] == aptitudes[eluCourrant] && (Constantes.ADRESSES_IP[i].compareTo(Constantes.ADRESSES_IP[eluCourrant]) < 0)) {
                    eluCourrant = i;
                }
            }
            elu = eluCourrant;
            //Envoi du message REPONSE
            ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_RESULTAT);
            byteBuffer.put(Constantes.RESULTAT);
            byteBuffer.putInt(elu);
            //Liste d'entier ou l'élément i vaut 1 si le site est présent dans la liste, 0 sinon
            int[] liste = new int[Constantes.NOMBRE_DE_SITES];
            liste[id] = 1;
            for (int i = 0; i < liste.length; ++i) {
                byteBuffer.putInt(liste[i]);
            }
            try {
                envoi(byteBuffer.array(), paquet);
                estEnElection = false;
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du datagramme");
                e.printStackTrace();
            }

        } else {
            ByteBuffer receivedDataBuffer = ByteBuffer.wrap(paquet.getData());
            ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_ANNONCE);
            receivedDataBuffer.put(byteBuffer.get()); //Type de message (ANNONCE)
            //On copie la liste du paquet reçu, sauf pour notre aptitude, qu'on met a jour
            for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
                if(i == id){
                    receivedDataBuffer.getInt();
                    byteBuffer.putInt(getAptitude());
                }else{
                    byteBuffer.putInt(receivedDataBuffer.getInt());
                }
            }
            try {
                envoi(byteBuffer.array(), paquet);
                estEnElection = false;
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du datagramme");
                e.printStackTrace();
            }
            estEnElection = true;
        }
    }

    /**
     * Traite un paquet reçu de type RESULTAT selon l'algorithme d'élection en
     * anneau avec pannes.
     * @param paquet
     */
    private void traiterResultat(DatagramPacket paquet) {
        ByteBuffer donneesEntieres = ByteBuffer.wrap(paquet.getData());
        int type = donneesEntieres.get();
        int eluPaquet = donneesEntieres.getInt();
        boolean[] dansListe = new boolean[Constantes.NOMBRE_DE_SITES];
        for (int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i) {
            dansListe[i] = donneesEntieres.getInt() == 1;
        }
        if (dansListe[id]) {
            //Fin
            return;
        } else if (!estEnElection && elu != eluPaquet) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_ANNONCE);
            //L'index de notre liste correspond au site, donc les sites non
            //initialisés ont une aptitude initiale de -1
            byteBuffer.put(Constantes.ANNONCE);
            for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
                if(i == id){
                    byteBuffer.putInt(getAptitude());
                }else{
                    byteBuffer.putInt(-1);
                }
            }
            try {
                envoi(byteBuffer.array(), paquet);
                estEnElection = false;
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du datagramme");
                e.printStackTrace();
            }
        } else if (estEnElection) {
            elu = eluPaquet;
            ByteBuffer receivedDataBuffer = ByteBuffer.wrap(paquet.getData());
            ByteBuffer byteBuffer = ByteBuffer.allocate(Constantes.TAILLE_TAMPON_ANNONCE);
            receivedDataBuffer.put(byteBuffer.get()); //Type de message (RESULTAT)
            //On copie la liste de sites ayant le résultat, en nous mettant à 1
            for(int i = 0; i < Constantes.NOMBRE_DE_SITES; ++i){
                if(i == id){
                    receivedDataBuffer.getInt();
                    byteBuffer.putInt(1);
                }else{
                    byteBuffer.putInt(receivedDataBuffer.getInt());
                }
            }
            try {
                envoi(byteBuffer.array(), paquet);
                estEnElection = false;
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi du datagramme");
                e.printStackTrace();
            }

            estEnElection = false;
        }

    }

    /**
     * Envoi un message au site suivant, en envoyant une quittance au précédent
     * si le paramètre paquetOriginal n'est pas nul.
     * @param corps
     * @param paquetOriginal
     * @throws Exception
     */
    private void envoi(byte[] corps, DatagramPacket paquetOriginal) throws Exception {

        //S'il vaut null, ça veut dire qu'on initialise une election
        if(paquetOriginal != null) {
            //On envoie la quittance au précédent
            envoiQuittance(paquetOriginal);
        }

        //On envoie le message au suivant
        DatagramSocket envoiSocket = new DatagramSocket();
        envoiSocket.send(new DatagramPacket(corps, corps.length,
                InetAddress.getByName(Constantes.ADRESSES_IP[idSuivant]), Constantes.PORTS[idSuivant]));

        //On attends la quittance du suivant avec timeout
        byte[] tampon = new byte[Constantes.TAILLE_TAMPON_QUITTANCE];
        DatagramPacket paquet = new DatagramPacket(tampon, tampon.length);
        try {
            envoiSocket.setSoTimeout(Constantes.MESSAGE_TIMEOUT);
            envoiSocket.receive(paquet);
        } catch (SocketTimeoutException e) {
            //La quittance n'a pas été reçue
            idSuivant = (idSuivant + 1) % Constantes.NOMBRE_DE_SITES;
            if (idSuivant == id) {
                //On a fait tout le tour de l'anneau
                idSuivant = (idSuivant + 1) % Constantes.NOMBRE_DE_SITES;
            }
            //Alors envoie le message au suivant
            envoi(corps, paquetOriginal);
        }

        if (paquet.getData()[0] == Constantes.QUITTANCE) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(paquet.getData());
            byte type = byteBuffer.get();
            int id = byteBuffer.getInt();
            if (id != idSuivant) {
                throw new Exception("Quittance reçue du mauvais site");
            }
        }
    }

    /**
     * Accesseur synchronizé de l'élu actuel d'un site.
     * @return l'identifiant du site élu actuel
     */
    public synchronized int getElu() {
        return elu;
    }

    /**
     * Méthode qui envoie une quittance au site qui a envoyé le paquet en paramètre
     *
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
    public int getAptitude() {
        return Constantes.PORTS[id] + Integer.parseInt(Constantes.ADRESSES_IP[id].substring(0, Constantes.ADRESSES_IP[id].lastIndexOf('.')));
    }

}
