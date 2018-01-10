package util;

interface Constantes {
    int PORT_DEFAUT = 6060;
    int NOMBRE_DE_SITES = 4;
    int TAILLE_TAMPON = NOMBRE_DE_SITES * 4 + 1 ;// 4 parce qu'un int est 32 bits et un octet en plus pour définir le type de message
    int MESSAGE_TIMEOUT = 200;
    int ELECTION_TIMEOUT = MESSAGE_TIMEOUT* NOMBRE_DE_SITES; //T unités de temps
}
