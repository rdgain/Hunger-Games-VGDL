package core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.sun.org.apache.xpath.internal.operations.Bool;
import core.competition.CompetitionParameters;
import core.content.SpriteContent;
import core.game.Game;
import ontology.Types;
import ontology.physics.ContinuousPhysics;
import ontology.physics.GridPhysics;
import ontology.physics.Physics;
import tools.Direction;
import tools.Utils;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 17/10/13
 * Time: 10:59
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public abstract class VGDLSprite {

    public int sizeIncrease = 1;

    /**
     * Name of this sprite.
     */
    public String name;

    /**
     * Indicates if this sprite is static or not.
     */
    public boolean is_static;

    /**
     * Indicates if passive movement is denied for this sprite.
     */
    public boolean only_active;

    /**
     * Indicates if this sprite is the avatar (player) of the game.
     */
    public boolean is_avatar;

    /**
     * Indicates if the sprite has a stochastic behaviour.
     */
    public boolean is_stochastic;

    /**
     * Color of this sprite.
     */
    public Color color;

    /**
     * States the pause ticks in-between two moves
     */
    public int cooldown;

    /**
     * Scalar speed of this sprite.
     */
    public double speed;
    
    /**
     * identifies whether this sprite should move (if an Avatar)
     */
    public boolean stationary;

    /**
     * Mass of this sprite (for Continuous physics).
     */
    public double mass;

    /**
     * Id of the type if physics this sprite responds to.
     */
    public int physicstype_id;

    /**
     * String that represents the physics type of this sprite.
     */
    public String physicstype;

    /**
     * Reference to the physics object this sprite belongs to.
     */
    public Physics physics;
    
    /**
     * The amount of gravity force that pushes down on the sprite
     */
    public double gravity;
    
    /**
     * The amount of friction this sprite has when moving.
     */
    public double friction;

    /**
     * Scale factor to draw this sprite.
     */
    public double shrinkfactor;

    /**
     * Indicates if this sprite has an oriented behaviour.
     */
    public boolean is_oriented;

    /**
     * Tells if an arrow must be drawn to indicate the orientation of the sprite.
     */
    public boolean draw_arrow;

    /**
     * Orientation of the sprite.
     */
    public Direction orientation;

    /**
     * Rectangle that this sprite occupies on the screen.
     */
    public Rectangle rect;

    /**
     * Rectangle occupied for this sprite in the previous game step.
     */
    public Rectangle lastrect;

    /**
     * Tells how many timesteps ago was the last move
     */
    public int lastmove;

    /**
     * Strength measure of this sprite.
     */
    public double strength;

    /**
     * Indicates if this sprite is a singleton.
     */
    public boolean singleton;

    /**
     * Indicates if this sprite is a resource.
     */
    public boolean is_resource;

    /**
     * Indicates if this sprite is a portal.
     */
    public boolean portal;

    /**
     * Indicates if the sprite is invisible. If it is, the effect is that
     * it is not drawn.
     */
    public String invisible;

    /**
     * If true, this sprite is never present in the observations passed to the controller.
     */
    public String hidden;
    
    /**
     * Indicates if the tile support autotiling
     */
    public boolean autotiling;
    
    /**
     * Indicates if the tile picking is random
     */
    public double randomtiling;
    
    /**
     * max frameRate for animating sprites
     */
    public double frameRate;
    
    /**
     * remaining frame speed
     */
    public double frameRemaining;
    
    /**
     * the current frame to be drawn
     */
    public int currentFrame;

    /**
     * If true, this sprite's functionality is disabled.
     * Disabled sprites are not drawn.
     * Information about disabled sprites can still be accessed.
     */
    private boolean disabled;

    /**
     * List of types this sprite belongs to. It contains the ids, including itself's, from this sprite up
     * in the hierarchy of sprites defined in SpriteSet in the game definition.
     */
    public ArrayList<Integer> itypes;

    /**
     * Indicates the amount of resources this sprite has, for each type defined as its int identifier.
     */
    public TreeMap<Integer, Integer> resources;

    /**
     * Image of this sprite.
     */
    public Image image;
    
    /**
     * Dictionary for autoTiling
     */
    public HashMap<Integer, Image> allImages;

    /**
     * String that represents the image in VGDL.
     */
    public String img;

    /**
     * Indicates if this sprite is an NPC.
     */
    public boolean is_npc;

    /**
     * ID of this sprite.
     */
    public int spriteID;

    /**
     * Indicates if this sprite was created by the avatar.
     */
    public boolean is_from_avatar;

    /**
     * Bucket
     */
    public int bucket;

    /**
     * Bucket remainder.
     */
    public boolean bucketSharp;

    /**
     * Indicates if the sprite is able to rotate in place.
     */
    public boolean rotateInPlace;

    /**
     * Indicates if the sprite is in its first cycle of existence.
     * Passive movement is not allowed in the first tick.
     */
    public boolean isFirstTick;

    /**
     * Health points of this sprite. It does not automatically kill the sprite
     * when it gets to 0 (an effect must do that, like 'SubtractHealthPoints').
     * Its default value is set to 0, if not set specifically in VGDL.
     */
    public int healthPoints;


    /**
     * Maximum health points of this sprite.
     * If not set specifically in VGDL, the default value is set to the healthPoints value set.
     * This is NOT the maximum possible amount of points, it's the max. ever had.
     */
    public int maxHealthPoints;


    /**
     * Limit of health points of this can have.
     * If not set specifically in VGDL, the default value is set to a very high value (1000)
     */
    public int limitHealthPoints;

    /**
     * Time to live for this sprite. Default set to -1 will not affect the sprite.
     * If ttl > -1, when it gets to 0, the sprite gets killed.
     */
    public int timeToLive = -1;
    
    /**
     * The sprites rotation
     */
    public double rotation;
    
    /**
     * The sprites size
     */
    public Dimension size;

    /**
     * Initializes the sprite, giving its position and dimensions.
     * @param position position of the sprite
     * @param size dimensions of the sprite on the screen.
     */
    protected void init(Vector2d position, Dimension size) {
        this.setRect(position, size);
        this.lastrect = new Rectangle(rect);
        physicstype_id = Types.PHYSICS_GRID;
        physics = null;
        gravity = 0.0;
        friction = 0.0;
        image = null;
        speed = 0;
        stationary = false;
        cooldown = 0;
        color = null;
        only_active = false;
        name = null;
        is_static = false;
        is_avatar = false;
        is_stochastic = false;
        is_from_avatar = false;
        mass = 1;
        shrinkfactor = 1.0;
        autotiling = false;
        randomtiling = -1;
        frameRate = -1;
        frameRemaining = 0;
        currentFrame = -1;
        allImages = new HashMap<Integer, Image>();
        is_oriented = false;
        draw_arrow = false;
        orientation = Types.DNONE;
        lastmove = 0;
        rotateInPlace = false;
        isFirstTick = true;
        disabled = false;
        limitHealthPoints = 1000;
        resources = new TreeMap<Integer, Integer>();
        itypes = new ArrayList<Integer>();
        rotation = 0.0;
        invisible = "false";
        
        this.size = size;


        determinePhysics(physicstype_id, size);
        setRandomColor();
    }

    public void setRect(Vector2d position, Dimension size)
    {
        Rectangle r = new Rectangle((int) position.x, (int) position.y, size.width, size.height);
        setRect(r);
    }


    public void setRect(Rectangle rectangle)
    {
        rect = new Rectangle(rectangle);
        bucket = rect.y / rect.height;
        bucketSharp = (rect.y % rect.height) == 0;
    }

    /**
     * Loads the default values for this sprite.
     */
    protected void loadDefaults() {
        name = this.getClass().getName();
    }

    /**
     * Sets a sampleRandom color for the sprite.
     */
    private void setRandomColor() {
        Random colorRnd = new Random();
        this.color = new Color((Integer) Utils.choice(Types.COLOR_DISC, colorRnd),
                (Integer) Utils.choice(Types.COLOR_DISC, colorRnd),
                (Integer) Utils.choice(Types.COLOR_DISC, colorRnd));
    }


    /**
     * Parses parameters for the sprite, received as a SpriteContent object.
     * @param content
     */
    public void parseParameters(SpriteContent content) {

        VGDLFactory factory = VGDLFactory.GetInstance();
        factory.parseParameters(content,this);
        
        determinePhysics(physicstype_id, size);

        //post-process. Some sprites may need to do something interesting (i.e. SpawnPoint) once their
        // parameters have been defined.
        this.postProcess();
    }

    /**
     * Determines the physics type of the game, creating the Physics objects that performs the calculations.
     * @param physicstype identifier of the physics type.
     * @param size dimensions of the sprite.
     * @return the phyics object.
     */
    private Physics determinePhysics(int physicstype, Dimension size) {
        this.physicstype_id = physicstype;
        switch (physicstype) {
            case Types.PHYSICS_GRID:
                physics = new GridPhysics(size);
                break;
            case Types.PHYSICS_CONT:
                physics = new ContinuousPhysics();
                break;
        }
        return physics;
    }

    /**
     * Updates this sprite, performing the movements and actions for the next step.
     * @param game the current game that is being played.
     */
    public void update(Game game)
    {
        updatePassive();
        if (timeToLive > -1) {
            if (timeToLive > 0) timeToLive--;
            else game.killSprite(this,false);
        }
    }

    /**
     * Set the disabled flag of this sprite.
     * @param is_disabled - disabled state
     */
    public void setDisabled(boolean is_disabled) {
        //System.out.println("disabled (real game? " + thinkingTime + ")"); //use for debugging
        disabled = is_disabled;
    }

    /**
     * Check if this sprite is disabled.
     * @return true if disabled, false otherwise.
     */
    public boolean is_disabled() { return disabled; }

    /**
     * Prepares the sprite for movement.
     */
    public void preMovement()
    {
        lastrect = new Rectangle(rect);
        lastmove += 1;

        frameRemaining -= 1;
        if (frameRate > 0 && frameRemaining <= 0 && allImages.size() > 0) {
            currentFrame = (currentFrame + 1) % allImages.size();
            frameRemaining = frameRate;
            image = allImages.get(currentFrame);
        }
    }

    /**
     * Updates this sprite applying the passive movement.
     */
    public void updatePassive() {

        if (!is_static && !only_active) {
            physics.passiveMovement(this);
        }
    }


    /**
     * Updates the orientation of the avatar to match the orientation parameter.
     * @param orientation final orientation the avatar must have.
     * @return true if orientation could be changed. This returns false in two circumstances:
     * the avatar is not oriented (is_oriented == false) or the previous orientation is the
     * same as the one received by parameter.
     */
    public boolean _updateOrientation(Direction orientation)
    {
        if(!this.is_oriented) return false;
        if(this.orientation.equals(orientation)) return false;
        this.orientation = orientation.copy();
        return true;
    }

    /**
     * Updates the position of the sprite, giving its orientation and speed.
     * @param orientation the orientation of the sprite.
     * @param speed the speed of the sprite.
     * @return true if the position changed.
     */
    public boolean _updatePos(Direction orientation, int speed) {
        if (speed == 0) {
            speed = (int) this.speed;
            if(speed == 0) return false;
        }

        if (cooldown <= lastmove && (Math.abs(orientation.x()) + Math.abs(orientation.y()) != 0)) {
        	rect.translate((int) (orientation.x() * speed), (int) (orientation.y() * speed));
        	updateBucket();
            lastmove = 0;
            return true;
        }
        return false;
    }
    
    /**
     * 
     * @param rot the rotation of the sprite
     * @return true if rotation could be changed
     */
    public boolean _updateRotation(double rot)
    {
        rotation = rot;
        return true;
    }

    public void updateBucket()
    {
        bucket = rect.y / rect.height;
        bucketSharp = (rect.y % rect.height) == 0;
    }

    /**
     * Returns the velocity of the sprite, in a Vector2d object.
     * @return the velocity of the sprite
     */
    public Vector2d _velocity() {
        if (speed == 0 || !is_oriented) {
            return new Vector2d(0, 0);
        } else {
            return new Vector2d(orientation.x() * speed, orientation.y() * speed);
        }
    }

    /**
     * Returns the last direction this sprite is following.
     * @return the direction.
     */
    public Vector2d lastDirection() {
        return new Vector2d(rect.getMinX() - lastrect.getMinX(),
                rect.getMinY() - lastrect.getMinY());
    }

    /**
     * Gets the position of this sprite.
     * @return the position as a Vector2d.
     */
    public Vector2d getPosition()
    {
        return new Vector2d(rect.x, rect.y);
    }

    public void setPosition(Vector2d position) {
        rect.x = (int) position.x;
        rect.y = (int) position.y;
    }


    /**
     * Gets the last position of this sprite. Returns null if same as current position.
     * @return the position as a Vector2d.
     */
    public Vector2d getLastPosition()
    {
        if (!lastrect.intersects(rect)) {
            return new Vector2d(lastrect.x, lastrect.y);
        }
        return null;
    }

    /**
     * Modifies the amount of resource by a given quantity.
     * @param resourceId id of the resource whose quantity must be changed.
     * @param amount_delta amount of units the resource has to be modified by.
     */
    public void modifyResource(int resourceId, int amount_delta)
    {
        int prev = getAmountResource(resourceId);
        int next = Math.max(0,prev + amount_delta);
        resources.put(resourceId, next);
    }

    /**
     * Removes all resources collected of the specified type.
     * @param resourceId - id of the resource whose quantity must be changed.
     */
    public void removeResource(int resourceId) {
        resources.put(resourceId, 0);
    }

    /**
     * Returns the amount of resource of a given type this sprite has.
     * @param resourceId id of the resource to check.
     * @return how much of this resource this sprite has.
     */
    public int getAmountResource(int resourceId)
    {
        int prev = 0;
        if(resources.containsKey(resourceId))
            prev = resources.get(resourceId);

        return prev;
    }

    /**
     * Draws this sprite (both the not oriented and, if appropriate, the oriented part)
     * @param gphx graphics object to draw in.
     * @param game reference to the game that is being played now.
     */
    public void draw(Graphics2D gphx, Game game) {
        String[] invis = invisible.split(",");

        boolean invis0 = Boolean.parseBoolean(invis[0]);
        boolean invis1;

        if (invis.length > 1)
            invis1 = Boolean.parseBoolean(invis[1]);
        else invis1 = invis0;

        boolean displayP1 = game.humanPlayer[0] && !invis0;
        boolean displayP2 = game.humanPlayer[1] && !invis1;

        boolean show;
        if (game.humanPlayer[0] && game.humanPlayer[1] || !game.humanPlayer[0] && !game.humanPlayer[1])
            if (invis0 == invis1) show = !invis0;
            else
                if (color == Types.DARKGRAY) show = false;
                else
                    show = !invis0 || !invis1;
        else
            show = displayP1 || displayP2;

        if(show && !disabled)
        {
            Rectangle r = new Rectangle(rect);

            if (!is_avatar || !is_oriented)
            {
	            if(image != null)
	                _drawImage(gphx, game, r);
	            else
	                _draw(gphx, game, r);
	
	            if(resources.size() > 0)
	            {
	                _drawResources(gphx, game, r);
	            }
	
	            if(healthPoints > 0)
	            {
	                _drawHealthBar(gphx, game, r);
	            }
            }

            else{
                _drawOriented(gphx, r);
	            if(resources.size() > 0)
	            {
	                _drawResources(gphx, game, r);
	            }
	
	            if(healthPoints > 0)
	            {
	                _drawHealthBar(gphx, game, r);
	            }
            }
        }
    }
    
    /*
    public void draw(Graphics2D gphx, Game game) {

        if(!invisible && !disabled)
        {
            Rectangle r = new Rectangle(rect);

            if(image != null)
                _drawImage(gphx, game, r);
            else
                _draw(gphx, game, r);

            if(resources.size() > 0)
            {
                _drawResources(gphx, game, r);
            }

            if(healthPoints > 0)
            {
                _drawHealthBar(gphx, game, r);
            }

            if (is_oriented)
                _drawOriented(gphx, r);
        }
    }
    */

    /**
     * In case this sprite is oriented and has an arrow to draw, it draws it.
     * @param g graphics device to draw in.
     */
    public void _drawOriented(Graphics2D g, Rectangle r)
    {
    	if(draw_arrow)
        {
            Color arrowColor = Color.WHITE; // new Color(color.getRed(), 255-color.getGreen(), color.getBlue());
            Polygon p = Utils.triPoints(r, orientation);


            // Rotation information
            
            if(shrinkfactor != 1)
            {
                r.width *= shrinkfactor;
                r.height *= shrinkfactor;
                r.x += (rect.width-r.width)/2;
                r.y += (rect.height-r.height)/2;
            }

            int w = image.getWidth(null);
            int h = image.getHeight(null);
            float scale = (float)r.width/w; //assume all sprites are quadratic.

            AffineTransform trans = new AffineTransform();
            trans.translate(r.x, r.y);
            trans.scale(scale,scale);
            trans.rotate(rotation,w/2.0,h/2.0);

            g.drawImage(image, trans, null);


            g.setColor(arrowColor);
            //g.drawPolygon(p);
            g.fillPolygon(p);
        }
    }

    /**
     * Draws the not-oriented part of the sprite
     * @param gphx graphics object to draw in.
     * @param game reference to the game that is being played now.
     */
    public void _draw(Graphics2D gphx, Game game, Rectangle r)
    {

        if(shrinkfactor != 1)
        {
            r.width *= shrinkfactor;
            r.height *= shrinkfactor;
            r.x += (rect.width-r.width)/2;
            r.y += (rect.height-r.height)/2;
        }

        gphx.setColor(color);

        if(is_avatar)
        {
            gphx.fillOval((int) r.getX(), (int) r.getY(), r.width, r.height);
        }else if(!is_static)
        {
            gphx.fillRect(r.x, r.y, r.width, r.height);
        }else
        {
            gphx.fillRect(r.x, r.y, r.width, r.height);
        }

    }

    /**
     * Draws the not-oriented part of the sprite, as an image. this.image must be not null.
     * @param gphx graphics object to draw in.
     * @param game reference to the game that is being played now.
     */
    public void _drawImage(Graphics2D gphx, Game game, Rectangle r)
    {
        if(shrinkfactor != 1)
        {
            r.width *= shrinkfactor;
            r.height *= shrinkfactor;
            r.x += (rect.width-r.width)/2;
            r.y += (rect.height-r.height)/2;
        }

        int w = image.getWidth(null);
        int h = image.getHeight(null);
        float scale = (float)r.width/w; //assume all sprites are quadratic.

        gphx.drawImage(image, r.x, r.y, (int) (w*scale), (int) (h*scale), null);

        //uncomment this to see lots of numbers around
        //gphx.setColor(Color.BLACK);
        //if(bucketSharp)   gphx.drawString("["+bucket+"]",r.x, r.y);
        //else              gphx.drawString("{"+bucket+"}",r.x, r.y);


    }

    /**
     * Draws the resources hold by this sprite, as an horizontal bar on top of the sprite.
     * @param gphx graphics to draw in.
     * @param game game being played at the moment.
     */
    protected void _drawResources(Graphics2D gphx, Game game, Rectangle r)
    {
        // if it's avatar, draw the resource bars at the top.


        int numResources = resources.size();
        double barheight = r.getHeight() / 6f / numResources;
        double offset = r.getMinY() + 2*r.height / 2f;

        Set<Map.Entry<Integer, Integer>> entries = resources.entrySet();
        for(Map.Entry<Integer, Integer> entry : entries)
        {
            int resType = entry.getKey();
            int resValue = entry.getValue();

            if(resType > -1) {
                double wiggle = r.width / 10.0f;
                double prop = Math.max(0, Math.min(1, resValue / (double) (game.getResourceLimit(resType))));

                Rectangle filled = new Rectangle((int) (r.x + wiggle / 2), (int) offset, (int) (prop * (r.width - wiggle)), (int) barheight);
                Rectangle rest = new Rectangle((int) (r.x + wiggle / 2 + prop * (r.width - wiggle)), (int) offset, (int) ((1 - prop) * (r.width - wiggle)), (int) barheight);

                gphx.setColor(game.getResourceColor(resType));
                gphx.fillRect(filled.x, filled.y, filled.width, filled.height);
                gphx.setColor(Types.BLACK);
                gphx.fillRect(rest.x, rest.y, rest.width, rest.height);
                offset += barheight;
            }
        }

    }


    /**
     * Draws the health bar, as a vertical bar on top (and left) of the sprite.
     * @param gphx graphics to draw in.
     * @param game game being played at the moment.
     * @param r rectangle of this sprite.
     */
    protected void _drawHealthBar(Graphics2D gphx, Game game, Rectangle r)
    {
        int maxHP = maxHealthPoints;
        if(limitHealthPoints != 1000)
            maxHP = limitHealthPoints;

        double wiggleX = r.width * 0.1f;
        double wiggleY = - r.height * 0.2f;
        double prop = Math.max(0,Math.min(1, healthPoints / (double) maxHP));

        double barWidth = r.width-wiggleX;
        int widthHealth = (int) (prop*barWidth);
        int widthUnhealth = (int) ((1-prop)*barWidth);
        int startX = (int) (r.getMinX()+wiggleX*0.5f);

        int barHeight = (int) (r.height * 0.2f);
        int yOffset = (int) (r.y+wiggleY * 1f);

        Rectangle filled = new Rectangle(startX, yOffset, widthHealth, barHeight);
        Rectangle rest   = new Rectangle(startX + widthHealth, yOffset, widthUnhealth, barHeight);

        gphx.setColor(this.color);
        gphx.fillRect(filled.x, filled.y, filled.width, filled.height);
        gphx.setColor(Types.BLACK);
        gphx.fillRect(rest.x, rest.y, rest.width, rest.height);
    }

    /**
     * Gets the unique and precise type of this sprite
     * @return the type
     */
    public int getType()
    {
        return itypes.get(itypes.size()-1);
    }

    /**
     * Method to perform post processing when the sprite has received its parameters.
     */
    public void postProcess()
    {
    	loadImage(img);

        if(!(this.orientation.equals(Types.DNONE)))
        {
            //Any sprite that receives an orientation, is oriented.
            this.is_oriented = true;
        }

        if(maxHealthPoints == 0)
            maxHealthPoints = healthPoints;
    }

    /**
     * Loads the image that represents this sprite, using its string name as reference.
     * @param str name of the image to load.
     */
    public void loadImage(String str)
    {
        if(image == null && str != null)
        {
            //load image.
            try {
            	if (this.autotiling || this.randomtiling >= 0 || this.frameRate >= 0){
            		if (str.contains(".png")) str = str.substring(0, str.length() - 3);
            		String imagePath = CompetitionParameters.IMG_PATH + str + "_";
            		boolean noMoreFiles = false;
            		int i = 0;
            		do{
            			String currentFile = imagePath + i + ".png";
            			if((new File(currentFile).exists())) {
            				allImages.put(i, ImageIO.read(new File(currentFile)));
    	                }
    	                else {
    	                    //System.out.println(currentFile);
    	                	//tempImage = ImageIO.read(this.getClass().getResource("/" + currentFile));
    	                	noMoreFiles = true;
    	                }
            			i += 1;
            		}while(!noMoreFiles);
            		image = allImages.get(0);
            	}
            	else{
	                if (!(str.contains(".png"))) str = str + ".png";
	                String image_file = CompetitionParameters.IMG_PATH + str;
	                if((new File(image_file).exists())) {
	                    image = ImageIO.read(new File(image_file));
	                }
	                else {
	                    //System.out.println(image_file);
	                    image = ImageIO.read(this.getClass().getResource("/" + image_file));
	                }
            	}

            } catch (IOException e) {
                System.out.println("Image " + str + " could not be found.");
                e.printStackTrace();
            } catch (Exception e) {
                //Ignore other exceptions.
                //If no images are shown, it'll draw an coloured rectangle instead.
            }
        }
    }

    /**
     * Used to indicate if this sprite was created by the avatar.
     * @param fromAvatar true if the avatar created this sprite.
     */
    public void setFromAvatar(boolean fromAvatar)
    {
        is_from_avatar = fromAvatar;
    }


    /**
     * Returns a string representation of this string, including its name and position.
     * @return the string representation of this sprite.
     */
    public String toString() {
        return name + " at (" + rect.getMinX() + "," + rect.getMinY() + ")";
    }

    /**
     * Creates a copy of this sprite. To be overwritten in each subclass.
     * @return  a copy of this sprite.
     */
    public abstract VGDLSprite copy();

    /**
     * Copies the attributes of this object to the one passed as parameter.
     * @param toSprite the sprite to copy to.
     */
    public void copyTo(VGDLSprite toSprite)
    {
        //this.color, this.draw_arrow don't need to be copied.
        toSprite.name = this.name;
        toSprite.is_static = this.is_static;
        toSprite.only_active = this.only_active;
        toSprite.disabled = this.disabled;
        toSprite.is_avatar = this.is_avatar;
        toSprite.is_stochastic = this.is_stochastic;
        toSprite.cooldown = this.cooldown;
        toSprite.speed = this.speed;
        toSprite.stationary = this.stationary;
        toSprite.mass = this.mass;
        toSprite.gravity = this.gravity;
        toSprite.friction = this.friction;
        toSprite.physicstype_id = this.physicstype_id;
        toSprite.physics = this.physics; //Object reference, but should be ok.
        toSprite.shrinkfactor = this.shrinkfactor;
        toSprite.is_oriented = this.is_oriented;
        toSprite.orientation = new Direction(orientation.x(), orientation.y());
        toSprite.rect = new Rectangle(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
        toSprite.lastrect =  new Rectangle(this.lastrect.x, this.lastrect.y, this.lastrect.width, this.lastrect.height);
        toSprite.lastmove = this.lastmove;
        toSprite.strength = this.strength;
        toSprite.singleton = this.singleton;
        toSprite.is_resource = this.is_resource;
        toSprite.portal = this.portal;
        toSprite.physicstype = this.physicstype;
        toSprite.color = this.color;
        toSprite.draw_arrow = this.draw_arrow;
        toSprite.is_npc = this.is_npc;
        toSprite.allImages = this.allImages;
        toSprite.image = this.image;
        toSprite.spriteID = this.spriteID;
        toSprite.is_from_avatar = this.is_from_avatar;
        toSprite.bucket = this.bucket;
        toSprite.bucketSharp = this.bucketSharp;
        toSprite.invisible = this.invisible;
        toSprite.autotiling = this.autotiling;
        toSprite.randomtiling = this.randomtiling;
        toSprite.frameRate = this.frameRate;
        toSprite.currentFrame = this.currentFrame;
        toSprite.frameRemaining = this.frameRemaining;
        toSprite.rotateInPlace = this.rotateInPlace;
        toSprite.isFirstTick = this.isFirstTick;
        toSprite.hidden = this.hidden;
        toSprite.healthPoints = this.healthPoints;
        toSprite.maxHealthPoints = this.maxHealthPoints;
        toSprite.limitHealthPoints = this.limitHealthPoints;
        toSprite.timeToLive = this.timeToLive;
        toSprite.rotation = this.rotation;

        toSprite.itypes = new ArrayList<Integer>();
        for(Integer it : this.itypes)
            toSprite.itypes.add(it);

        toSprite.resources = new TreeMap<Integer, Integer>();
        Set<Map.Entry<Integer, Integer>> entries = this.resources.entrySet();
        for(Map.Entry<Integer, Integer> entry : entries)
        {
            toSprite.resources.put(entry.getKey(), entry.getValue());
        }

    }

    /**
     * Determines if two the object passed is equal to this.
     * We are NOT overriding EQUALS on purpose (Costly operation for eventHandling).
     */
    public boolean equiv(Object o)
    {
        if(this == o) return true;
        if(!(o instanceof VGDLSprite)) return false;
        VGDLSprite other = (VGDLSprite)o;

        if(other.name != this.name) return false;
        if(other.is_static != this.is_static) return false;
        if(other.only_active != this.only_active) return false;
        if(other.disabled != this.disabled) return false;
        if(other.is_avatar != this.is_avatar) return false;
        if(other.is_stochastic != this.is_stochastic) return false;
        if(other.cooldown != this.cooldown) return false;
        if(other.speed != this.speed) return false;
        if(other.stationary != this.stationary) return false;
        if(other.mass != this.mass) return false;
        if(other.physicstype_id != this.physicstype_id) return false;
        if(other.gravity != this.gravity) return false;		
        if(other.friction != this.friction) return false;
        if(other.shrinkfactor != this.shrinkfactor) return false;
        if(other.is_oriented != this.is_oriented) return false;
        if(!other.orientation.equals(this.orientation)) return false;
        if(!other.rect.equals(this.rect)) return false;
        if(other.lastmove != this.lastmove) return false;
        if(other.strength != this.strength) return false;
        if(other.singleton != this.singleton) return false;
        if(other.is_resource != this.is_resource) return false;
        if(other.portal != this.portal) return false;
        if(other.is_npc != this.is_npc) return false;
        if(other.is_from_avatar != this.is_from_avatar) return false;
        if(other.invisible != this.invisible) return false;
        if(other.autotiling != this.autotiling) return false;
        if(other.randomtiling != this.randomtiling) return false;
        if(other.frameRate != this.frameRate) return false;
        if(other.spriteID != this.spriteID) return false;
        if(other.isFirstTick != this.isFirstTick) return false;
        if(!Objects.equals(other.hidden, this.hidden)) return false;
        if(other.healthPoints != this.healthPoints) return false;
        if(other.maxHealthPoints != this.maxHealthPoints) return false;
        if(other.limitHealthPoints != this.limitHealthPoints) return false;

        int numTypes = other.itypes.size();
        if(numTypes != this.itypes.size()) return false;
        for(int i = 0; i < numTypes; ++i)
            if(other.itypes.get(i) != this.itypes.get(i)) return false;

        return true;
    }

    /**
     * Get all sprites that affect or being affected by the current sprite
     * @return a list of all dependent sprites
     */
    public ArrayList<String> getDependentSprites(){
    	return new ArrayList<String>();
    }
    
}
