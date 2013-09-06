package it.robertof.rpm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Wanna some cool colors? Enjoy.
 * (see {@link HoloColorSequenceGenerator#randomColor()})
 * @author Robertof
 */
public class HoloColorSequenceGenerator
{
    private int[] colorArray = new int[] {
            android.R.color.holo_blue_dark, android.R.color.holo_blue_light,
            android.R.color.holo_green_dark, android.R.color.holo_green_light,
            android.R.color.holo_orange_dark, android.R.color.holo_orange_light,
            android.R.color.holo_red_dark, android.R.color.holo_red_light
    };
    private List<Integer> generatedColorList = new ArrayList<Integer>();
    private Random generator = new Random();
    
    /**
     * Generates a random holo-ish color.
     * NOTE: the generator won't return already
     * generated colors, even from a different brightness (i.e.
     * if you got holo_blue_dark, you can't get holo_blue_dark nor
     * holo_blue_light). Keep the instance of the HoloColorSequenceGenerator
     * if you want this to work.
     * @return A random resource color from {@link android.R.color}
     */
    public int randomColor()
    {
        int rand = generator.nextInt (colorArray.length);
        int operator = ( rand % 2 == 0 ? 1 : -1 );
        if (generatedColorList.size() == colorArray.length / 2)
            generatedColorList.clear();
        else if (generatedColorList.contains (rand) || generatedColorList.contains (rand + operator))
            return randomColor();
        generatedColorList.add (rand);
        return colorArray[rand];
    }
    
    public void addToAlreadyGenerated (int resId)
    {
        int pos = this.indexOfInColorArray (resId);
        if (pos == -1) return;
        if (!generatedColorList.contains (pos))
            generatedColorList.add (pos);
    }
    
    private int indexOfInColorArray (int value)
    {
        for (int i = 0; i < colorArray.length; i++)
        {
            if (colorArray[i] == value) return i;
        }
        return -1;
    }
}
