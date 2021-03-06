package com.davidameny.flappybird;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;

import java.util.Random;

public class FlappyBird extends ApplicationAdapter {
	private SpriteBatch batch;
	private Texture background;
	private Texture topTube, bottomTube;
	private Texture gameOver;

	private TextureRegion[] animationFrames;
	private Texture birdSpriteSheet;
	private Animation animation;
	private float elapsedTime;
	private BitmapFont font;

	private Circle birdCollider;
	private Rectangle[] bottomTubeColliders;
    private Rectangle[] topTubeColliders;

    private int score = 0;
    private int scoringTube = 0;
	private int gameState = 0;      // 0=idle, 1=playing, 2=game over
    private float birdX, birdY;
    private int birdHeight, birdWidth;
    private float birdVelocity = 0;
    private int numTubes = 4;
    private Tube[] tubes;

    private float maxOffset, minOffset;
    private Random gapOffSet;

    private float tubeXVelocity = 400;
    private float gravity = 20;
    private float gap = 600;
    private int bumpSpeed = 20;
    private int tubeSpacing;

    private int screenWidth;
    private int screenHeight;
    private int tubeWidth;
    private int tubeHeight;

    private static final int MIN_TUBE_POS = 200;
    private static final int FONT_SIZE = 200;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
		background = new Texture("bg.png");
		gameOver = new Texture("flappybird_gameover.png");

		birdSpriteSheet = new Texture("birdspritesheet.png");
		topTube = new Texture("toptube.png");
		bottomTube = new Texture("bottomtube.png");

		TextureRegion[][] tmpFrames = TextureRegion.split(birdSpriteSheet,136, 96);
        animationFrames = new TextureRegion[2];

        int index = 0;

        for (int i = 0; i < 2; i++){
            animationFrames[index++] = tmpFrames[0][i];
        }

        animation = new Animation(1f/10f, animationFrames);

        birdHeight = animationFrames[0].getRegionHeight();
        birdWidth = animationFrames[0].getRegionWidth();

        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        tubeWidth = bottomTube.getWidth();
        tubeHeight = bottomTube.getHeight();
        maxOffset = -(tubeHeight + gap + MIN_TUBE_POS - screenHeight);
        minOffset = -(tubeHeight - MIN_TUBE_POS);

        gapOffSet = new Random();
        tubeSpacing = screenWidth * 3/4;

        // Generates the colliders
        bottomTubeColliders = new Rectangle[numTubes];
        topTubeColliders = new Rectangle[numTubes];

        for(int i = 0; i < numTubes; i++){
            bottomTubeColliders[i] = new Rectangle();
            topTubeColliders[i] = new Rectangle();
        }

        birdCollider = new Circle();
        tubes = new Tube[numTubes];

        // Converts the true type font to a bitmap font
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("District.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = FONT_SIZE;
        parameter.characters = "0123456789";
        font = generator.generateFont(parameter);
        font.setColor(Color.WHITE);
        generator.dispose();

        startGame();
	}

	@Override
	public void render () {
	    float deltaTime = Gdx.graphics.getDeltaTime();
	    elapsedTime += deltaTime;

        batch.begin();
        batch.draw(background, 0, 0, screenWidth, screenHeight);

        if(gameState == 1){
            if (Gdx.input.justTouched()){
                birdVelocity += bumpSpeed;
            }

            // Some basic physics
            birdVelocity = birdVelocity - gravity * deltaTime;

            float newY = birdY + birdVelocity;

            // Pegs the bird at the top of the screen if it is going higher than the top
            // since the colliders don't extend beyond the screen
            if(newY <= (screenHeight - birdHeight)){
                birdY = newY;
            } else {
                birdY = screenHeight - birdHeight;
                birdVelocity = 0;
            }

            if (birdY <= 0 ){
                gameState = 2;
            }

            // increments score as player passing between pipes
            if (tubes[scoringTube].getXPos() <= (screenWidth / 2 - tubeWidth /2)) {
                score++;
                scoringTube = (scoringTube >= (numTubes - 1))? 0: scoringTube + 1;
                Gdx.app.log("Score", Integer.toString(score));
            }

            // Update the position of the tubes
            for (int i = 0; i < numTubes; i++){
                tubes[i].setXPos(tubes[i].getXPos() - tubeXVelocity * deltaTime);

                float newTubeXPos = tubes[i].getXPos();
                float offset = tubes[i].getOffset();

                batch.draw(bottomTube, newTubeXPos, offset);
                batch.draw(topTube, newTubeXPos, offset + tubeHeight + gap);

                if (newTubeXPos < -tubeWidth){
                    tubes[i].setXPos(newTubeXPos + numTubes * tubeSpacing);
                    tubes[i].setOffset(generateOffset());
                }

                bottomTubeColliders[i].set(newTubeXPos, offset, tubeWidth, tubeHeight);
                topTubeColliders[i].set(newTubeXPos, offset + tubeHeight + gap, tubeWidth, tubeHeight);
            }

            birdCollider.set(screenWidth/2, birdY + birdHeight/2, birdWidth/2);

            for (int i = 0; i < numTubes; i++) {
                if (Intersector.overlaps(birdCollider, bottomTubeColliders[i]) || Intersector.overlaps(birdCollider, topTubeColliders[i])) {
                    Gdx.app.log("Collision", "Detected");
                    gameState = 2;
                }
            }
        } else if (gameState == 0) {
            if (Gdx.input.justTouched()){
                Gdx.app.log("Input", "Touch Logged!");
                gameState = 1;
            }
        } else if (gameState == 2){
            batch.draw(gameOver, screenWidth / 2 - gameOver.getWidth()/2, screenHeight * 2 / 3 - gameOver.getHeight()/2);
            if(Gdx.input.justTouched()){
                gameState = 1;
                startGame();
            }
        }

        if (gameState != 2) {
            batch.draw((TextureRegion) animation.getKeyFrame(elapsedTime, true), birdX, birdY);
        }

        font.draw(batch, Integer.toString(score), 50, screenHeight - 25);
        batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		background.dispose();
		gameOver.dispose();
		birdSpriteSheet.dispose();
		topTube.dispose();
		bottomTube.dispose();
		font.dispose();
	}

	private void startGame(){
	    birdVelocity = 0;
	    scoringTube = 0;
	    score = 0;

        birdX = screenWidth / 2 - birdWidth / 2;
        birdY = screenHeight / 2 - birdHeight / 2;

        for (int i = 0; i < numTubes; i++){
            tubes[i] = new Tube(screenWidth + i * tubeSpacing, generateOffset());
        }
    }

	private float generateOffset(){
        return minOffset + (maxOffset - minOffset) * gapOffSet.nextFloat();
    }
}

class Tube {
    private float xPos;
    private float offset;

    Tube(float xPos, float offset) {
        this.xPos = xPos;
        this.offset = offset;
    }

    float getXPos() {
        return xPos;
    }

    void setXPos(float xPos) {
        this.xPos = xPos;
    }

    float getOffset() {
        return offset;
    }

    void setOffset(float offset) {
        this.offset = offset;
    }
}
