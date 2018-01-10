package util;

import java.net.InetAddress;

public interface Constantes {
    int PORT_DEFAUT = 6060;
    int NOMBRE_DE_SITES = 4;
    int TAILLE_TAMPON_ANNONCE = Byte.BYTES + NOMBRE_DE_SITES * Integer.BYTES ;//un octet pour le type de message + Nombre de site * taille d'un int pour connaitre la taille de la liste d'aptitude
    int TAILLE_TAMPON_RESULTAT = Byte.BYTES + Integer.BYTES + NOMBRE_DE_SITES * Integer.BYTES ;//type de message + elu + liste des sites ayant vu le resultat
    int TAILLE_TAMPON = Math.max(TAILLE_TAMPON_ANNONCE,TAILLE_TAMPON_RESULTAT);
    int TAILLE_TAMPON_QUITTANCE = Integer.BYTES; //Id de celui qui envoie la quittance pour signifier qu'il a bien recu la liste
    int MESSAGE_TIMEOUT = 200;
    int ELECTION_TIMEOUT = MESSAGE_TIMEOUT* NOMBRE_DE_SITES; //T unités de temps

    // Types de messages
    byte ANNONCE = (byte) 0;
    byte RESULTAT = (byte) 1;
    byte QUITTANCE = (byte) 2;

}
