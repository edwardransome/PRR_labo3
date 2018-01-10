import machine.Site;
import util.Constants;

public class Main {
    public static void main(String[] args) {
        if(args.length != 1){
            System.err.println("Passez l'id du site a démarrer comme unique paramètre");
        }else{
            int id = Integer.parseInt(args[0]);
            Site site = new Site(id, Constants.BASE_PORT + id);
        }

    }
}
