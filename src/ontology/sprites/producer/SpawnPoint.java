package ontology.sprites.producer;

import java.awt.Dimension;
import java.util.ArrayList;

import com.sun.org.apache.xpath.internal.SourceTree;
import core.VGDLRegistry;
import core.VGDLSprite;
import core.content.SpriteContent;
import core.game.Game;
import ontology.Types;
import tools.Direction;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 21/10/13
 * Time: 18:24
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class SpawnPoint extends SpriteProducer
{
    public double prob;
    public int total;
    public int counter;
    public int respawn;
    public int counterRespawn;
    public String stype;
    public String follow;
    public int itype;
    public int ifollow;
    public Direction spawnorientation;

    private int start;

    public SpawnPoint(){}

    public SpawnPoint(Vector2d position, Dimension size, SpriteContent cnt)
    {
        //Init the sprite
        this.init(position, size);

        //Specific class default parameter values.
        loadDefaults();

        //Parse the arguments.
        this.parseParameters(cnt);
    }

    protected void loadDefaults()
    {
        super.loadDefaults();
        prob = 1.0;
        total = 0;
        start = -1;
        color = Types.BLACK;
        cooldown = 1;
        is_static = true;
        spawnorientation = Types.DNONE;
        itype = -1;
        ifollow = -1;
        respawn = -1;
    }

    public void postProcess()
    {
        super.postProcess();
        is_stochastic = (prob > 0 && prob < 1);
        counter = 0;
        counterRespawn = 0;
        if(stype != null) //Could be, if we're using different stype variants in subclasses.
            itype = VGDLRegistry.GetInstance().getRegisteredSpriteValue(stype);
        if(follow != null) //Could be, if we're using different stype variants in subclasses.
            ifollow = VGDLRegistry.GetInstance().getRegisteredSpriteValue(follow);
    }

    public void update(Game game)
    {
        if(start == -1)
            start = game.getGameTick();

        float rollDie = game.getRandomGenerator().nextFloat();
        if(((start+game.getGameTick()) % cooldown == 0) && rollDie < prob)
        {
            if (itype != -1) {
                VGDLSprite newSprite = game.addSprite(itype, this.getPosition());
                if (newSprite != null) {
                    counter++;

                    //We set the orientation given by default it this was passed.
                    if (!(spawnorientation.equals(Types.DNONE)))
                        newSprite.orientation = spawnorientation.copy();
                        //If no orientation given, we set the one from the spawner.
                    else if (newSprite.orientation.equals(Types.DNONE))
                        newSprite.orientation = this.orientation.copy();
                }
            } else {
                System.out.println("Error: spawn type not found");
            }
        }

        super.update(game);

        if(total > 0 && counter >= total)
        {
            //boolean variable set to false to indicate the sprite was not transformed
            if (respawn > 0) {
                if (counterRespawn < respawn) counterRespawn++;
                else {
                    counter = 0;
                    counterRespawn = 0;
                }
            } else {
                game.killSprite(this, false);
            }
        }

        if (follow != null) {
            ArrayList<VGDLSprite> sprites = new ArrayList<>();
            for (int i : game.getSubTypes(ifollow)) {
                ArrayList<VGDLSprite> sp = game.getSprites(i);
                if (!sp.isEmpty()) {sprites = sp; break;}
            }

            if (!sprites.isEmpty())  this.setPosition(sprites.get(0).getPosition());
        }
    }

    /**
     * Updates spawn itype with newitype
     * @param itype - current spawn type
     * @param newitype - new spawn type to replace the first
     */
    public void updateItype(int itype, int newitype) {
        this.itype = newitype;
    }

    public VGDLSprite copy()
    {
        SpawnPoint newSprite = new SpawnPoint();
        this.copyTo(newSprite);
        return newSprite;
    }

    public void copyTo(VGDLSprite target)
    {
        SpawnPoint targetSprite = (SpawnPoint) target;
        targetSprite.prob = this.prob;
        targetSprite.total = this.total;
        targetSprite.counter = this.counter;
        targetSprite.stype = this.stype;
        targetSprite.follow = this.follow;
        targetSprite.itype = this.itype;
        targetSprite.ifollow = this.ifollow;
        targetSprite.spawnorientation = this.spawnorientation.copy();
        targetSprite.start = this.start;
        super.copyTo(targetSprite);
    }

    @Override
    public ArrayList<String> getDependentSprites(){
    	ArrayList<String> result = new ArrayList<String>();
        if(stype != null) result.add(stype);
        if(follow != null) result.add(follow);
    	
    	return result;
    }
}
