/*
 -------------------------------------------------------------------------------
 Laboratoire : Laboratoire 3 - PRR
 Fichier     : Main.java
 Auteur(s)   : Michael Spierer & Edward Ransome
 Date        : 10.01.2017

 Dans ce laboratoire, il est question d'implémenter un algorithme d'élection
 avec panne possible des sites en supposant un réseau entièrement fiable
 connectant ceux-cis. Le nombre total de sites est d'au plus 4.

 Chaque site connaît son identifiant ainsi que l'identifiant et adresse/port des
 autres sites. l'identifiant du prédecesseur d'un site est à priori
 l'identifiant de ce site - 1 modulo 4. Le successeur d'un site est son
 identifiant + 1 modulo 4.

 Il existe trois types de messages: ANNONCE, permettant l'annonce des sites
 et de leurs aptitudes lors d'une élection. RESULTAT, qui diffuse le site de
 plus grande aptitude dans le réseau ainsi qu'une liste des sites connaissants
 cette information, et QUITTANCE, un message de quittance devant être envoyé
 à l'expéditeur d'un message lors de sa réception par un site.

 La structure des messages est ainsi: tout message commence avec un octet
 décrivant le type du message. Dans le cas d'une ANNONCE, il en suite quatre
 entiers (Donc quatre fois 4 octets) valant -1 si aucune aptitude n'a été
 associée à un site, et valant une aptitude sinon. Dans le cas d'un message
 RESULTAT, la liste de l'algorithme est remplacée par de nouveau quatre entiers.
 Si le l'entier à l'index i vaut 1, le site i est dans la liste, sinon il
 l'entier vaut 0 et il ne l'est pas. Pour la QUITTANCE, il y a en plus de
 l'octet du type un entier valant l'identifiant de l'expediteur. Avec ces
 structures de paquets, on économise beaucoup de place comparé à l'utilisation
 par exemple de List Java ou pire, des Map<Site, Aptitude>.

 Si un site tombe en panne avant d'envoyer une quittance à l'expéditeur d'un
 message lui étant destiné, la panne est detectée par la durée
 MESSAGE_TIMEOUT, et le site ayant envoyé le message déduira que son successeur
 est en panne. Il va ensuite incrémenter son successeur et réessayer. Si un site
 tombe en panne après l'envoi de quittance mais avant la propagation d'une
 annonce, la panne sera détectée par le site ayant lancé l'élection: le temps
 ELECTION_TIMEOUT, qui représente le temps maximum que doivent prendre les
 messages à circuler dans tout l'anneau, sera dépassé.



 -------------------------------------------------------------------------------
 */
import machine.Site;
import util.Constantes;

/**
 * Fonction amorce permettant d'initialiser un site en passant son id en
 * paramètre de la ligne de commande.
 */
public class Main {
    public static void main(String[] args) {
        if(args.length != 1){
            System.err.println("Passez l'id du site à démarrer comme unique paramètre");
        }else{
            int id = Integer.parseInt(args[0]);
            Site site = new Site(id, Constantes.PORTS[id]);
        }

    }
}
