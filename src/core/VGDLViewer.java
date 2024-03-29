package core;

import java.awt.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComponent;

import core.game.Game;
import core.player.AbstractPlayer;
import core.player.Player;
import ontology.Types;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 24/10/13
 * Time: 10:54
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class VGDLViewer extends JComponent
{
    /**
     * Reference to the game to be painted.
     */
    public Game game;

    /**
     * Dimensions of the window.
     */
    private Dimension size;

    /**
     * Sprites to draw
     */
    public SpriteGroup[] spriteGroups;

    /**
     * Player of the game
     */
    public Player[] players;

    int promptDelay = 10; // number of frames the prompt will be displayed
    int delayCounter;
    int fontsize = 30;

    ArrayList<String> prompts;


    /**
     * Creates the viewer for the game.
     * @param game game to be displayed
     */
    public VGDLViewer(Game game, Player[] players)
    {
        this.game = game;
        this.size = game.getScreenSize();
        this.players = players;
        delayCounter = 0;
        prompts = new ArrayList<>();
    }

    /**
     * Main method to paint the game
     * @param gx Graphics object.
     */
    public void paintComponent(Graphics gx)
    {
        Graphics2D g = (Graphics2D) gx;

        //For a better graphics, enable this: (be aware this could bring performance issues depending on your HW & OS).
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        //g.setColor(Types.LIGHTGRAY);
        g.setColor(Types.BLACK);
        g.fillRect(0, size.height, size.width, size.height);

        //Possible efficiency improvement: static image with immovable objects.
        /*
        BufferedImage mapImage = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D gImage = mapImage.createGraphics();
        */

        try {
            int[] gameSpriteOrder = game.getSpriteOrder();
            if (this.spriteGroups != null) for (Integer spriteTypeInt : gameSpriteOrder) {
                if (spriteGroups[spriteTypeInt] != null) {
                    ArrayList<VGDLSprite> spritesList = spriteGroups[spriteTypeInt].getSprites();
                    for (VGDLSprite sp : spritesList) {
                        if (sp != null) sp.draw(g, game);
                    }

                }
            }
        }catch(Exception e) {}

        g.setColor(Types.BLACK);
        for (Player player : players)
            player.draw(g);

        // Draw effect prompts
        if (delayCounter == 0) {
            prompts.addAll(game.nextPrompts);
            game.nextPrompts.clear();
        } else if (delayCounter < promptDelay) {
            for (String s : prompts) {
                int x = getWidth()/2 - (s.length()/2) * fontsize / 4;
                int y = getHeight()/2 - fontsize / 4;
                g.setFont(new Font("TimesRoman", Font.BOLD, fontsize)); ;
                g.setColor(Color.WHITE);
                g.drawString(s, x, y);
            }
        } else {
            delayCounter = 0;
            prompts.clear();
        }
        if (!prompts.isEmpty()) {
            delayCounter++;
        }

    }



    /**
     * Paints the sprites.
     * @param spriteGroupsGame sprites to paint.
     */
    public void paint(SpriteGroup[] spriteGroupsGame)
    {
        //this.spriteGroups = spriteGroupsGame;
        this.spriteGroups = new SpriteGroup[spriteGroupsGame.length];
        for(int i = 0; i < this.spriteGroups.length; ++i)
        {
            this.spriteGroups[i] = new SpriteGroup(spriteGroupsGame[i].getItype());
            this.spriteGroups[i].copyAllSprites(spriteGroupsGame[i].getSprites());
        }

        this.repaint();
    }

    /**
     * Gets the dimensions of the window.
     * @return the dimensions of the window.
     */
    public Dimension getPreferredSize() {
        return size;
    }

}