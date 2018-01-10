import machine.Site;
import util.Constantes;

public class Main {
    public static void main(String[] args) {
        if(args.length != 1){
            System.err.println("Passez l'id du site a démarré comme unique paramètre");
        }else{
            int id = Integer.parseInt(args[0]);
            Site site = new Site(id, Constantes.PORT_DEFAUT + id);
        }

    }
}
