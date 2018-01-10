package util;

import java.net.InetAddress;

public interface Constantes {
    int PORT_DEFAUT = 6060;
    int NOMBRE_DE_SITES = 4;
    int TAILLE_TAMPON = NOMBRE_DE_SITES * Integer.BYTES + Byte.BYTES;// Nombre de site * taille d'un int pour connaitre la taille de la liste d'aptitude et un octet en plus pour définir le type de message
    int MESSAGE_TIMEOUT = 200;
    int ELECTION_TIMEOUT = MESSAGE_TIMEOUT* NOMBRE_DE_SITES; //T unités de temps

    // Types de messages
    byte ANNONCE = (byte) 0;
    byte RESULTAT = (byte) 1;

}
