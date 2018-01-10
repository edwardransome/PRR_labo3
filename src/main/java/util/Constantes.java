package util;

import java.net.InetAddress;

/**
 * Interface contenant les constantes de l'application permettant de les
 * modifier facilement.
 */
public interface Constantes {
    //Les ports utilisés par les sites 0-3
    int[] PORTS = {6060, 6061, 6062, 6063};
    //Les adresses IP utilisées par les sites 0-3
    String[] ADRESSES_IP = {"127.0.0.1", "127.0.0.1", "127.0.0.1", "127.0.0.1"};
    //Le nombre total de sites dans la topologie
    int NOMBRE_DE_SITES = ADRESSES_IP.length;
    //Un octet pour le type de message + taille de la liste d'aptitude
    //La liste d'aptitude contient un entier par site
    int TAILLE_TAMPON_ANNONCE = Byte.BYTES + NOMBRE_DE_SITES * Integer.BYTES;
    //Type de message + identifiant de l'elu + liste des sites ayant vu le resultat
    int TAILLE_TAMPON_RESULTAT = Byte.BYTES + Integer.BYTES + NOMBRE_DE_SITES * Integer.BYTES;
    //Taille d'un buffer pouvant recevoir tout type de message
    int TAILLE_TAMPON = Math.max(TAILLE_TAMPON_ANNONCE, TAILLE_TAMPON_RESULTAT);
    //Id de celui qui envoie la quittance pour signifier qu'il a bien recu la liste
    int TAILLE_TAMPON_QUITTANCE = Integer.BYTES;
    //Timeout arbitraire pour la réception d'une quittance
    int MESSAGE_TIMEOUT = 200;
    //Timeout pour qu'une annonce parcourt l'anneau entier
    int ELECTION_TIMEOUT = MESSAGE_TIMEOUT * NOMBRE_DE_SITES; //T unités de temps

    //Types de message
    byte ANNONCE = (byte) 0;
    byte RESULTAT = (byte) 1;
    byte QUITTANCE = (byte) 2;

}
