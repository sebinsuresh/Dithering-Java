/* 
 * Sebin Puthenthara Suresh
 * github.com/sebinsuresh
 * A Java program that produces dithered images using BufferedImage class.
 * Output may be exported as an image file or printed in the console, given the width of the console in number of chars.
 * Usage:
 * java Dither filename.filetype -> will produce a dithered image in the same folder
 */
import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Dither {
    public static void main (String[] args){
        BufferedImage img;
        try{
            img = ImageIO.read(new File(args[0]));
            // If the input thorough args was only the filename
            if(args.length == 1){
                generateDitheredImage(img, args[0]);
            }
        } catch (Exception ex){
            System.out.println("Image Read Failed. \n" + 
                "Make sure the image exists in the same root folder and the filename is typed correctly, including extensions.");
            ex.printStackTrace();
        }
    }

    // Uses stucki dithering as found on:
    // http://www.tannerhelland.com/4660/dithering-eleven-algorithms-source-code/
    public static void generateDitheredImage(BufferedImage img, String filename){
        int height = img.getHeight();
        int width = img.getWidth();
        System.out.println(filename + " " + width + "x" + height + " " + img.getType());
        
        BufferedImage output = new BufferedImage(width, height, img.getType());
        for(int i=0; i < height; i++){
            for(int j = 0; j < width; j++){
                int color = img.getRGB(j,i);
                int alpha = (color&0xff000000)>>24;
                if(alpha == 0){
                    img.setRGB(j,i,0xffffffff);
                }
            }
        }
        for(int i=0; i < height; i++){
            for(int j = 0; j < width; j++){
                // The getRGB method returns RGBA values in the default TYPE_INT_ARGB type.
                // This gives 8 bit RGBA color components packed into one integer, in the order Alpha,Red,Green,Blue
                // Eg: 00000000 00000100 00000100 00000100 -> (r,g,b,a) = (4,4,4,0) -> you get 1092 in decimal int.
                // I'll use bit masking & shifting to get individual values.
                int color = img.getRGB(j,i);
                
                int greyValue = getGreyFromRGB(color);

                int newColor = 0; 
                double error = 0;
                if(greyValue < 128){
                    error = (greyValue)/42;
                    newColor = 0xff000000; //Black
                } else {
                    error = (greyValue-255)/42;
                    newColor = 0xffffffff; //white
                }

                //Same row
                if(j < width-1){ //there is a column to right
                    int neighbor = getGreyFromRGB(img.getRGB(j+1,i));
                    img.setRGB(j+1,i, getGSRGBfromGrey(neighbor + (error*8))); //error * 8
                }
                if(j < width-2){ //there are two columns to the right
                    int neighbor = getGreyFromRGB(img.getRGB(j+2,i));
                    img.setRGB(j+2,i, getGSRGBfromGrey(neighbor + (error*4))); //error * 4
                }
                
                //there is a row below
                if(i < height-1){
                    int neighbor = getGreyFromRGB(img.getRGB(j,i+1));
                    img.setRGB(j,i+1, getGSRGBfromGrey(neighbor + (error*8))); //error * 8

                    if(j < width-1){ //there is a column to right
                        neighbor = getGreyFromRGB(img.getRGB(j+1,i+1));
                        img.setRGB(j+1,i+1, getGSRGBfromGrey(neighbor + (error*4))); //error * 4
                    }
                    if(j < width-2){ //there are two columns to the right
                        neighbor = getGreyFromRGB(img.getRGB(j+2,i+1));
                        img.setRGB(j+2,i+1, getGSRGBfromGrey(neighbor + (error*2))); //error * 2
                    }

                    if(j > 0){ //there is a column to left
                        neighbor = getGreyFromRGB(img.getRGB(j-1,i+1));
                        img.setRGB(j-1,i+1, getGSRGBfromGrey(neighbor + (error*4))); //error * 4
                    }
                    if(j > 1){ //there are two columns to the left
                        neighbor = getGreyFromRGB(img.getRGB(j-2,i+1));
                        img.setRGB(j-2,i+1, getGSRGBfromGrey(neighbor + (error*2))); //error * 2
                    }
                }

                //there are two rows below
                if(i < height-2){ 
                    int neighbor = getGreyFromRGB(img.getRGB(j,i+2));
                    img.setRGB(j,i+2, getGSRGBfromGrey(neighbor + (error*4))); // error * 4

                    if(j < width-1){ //there is a column to right
                        neighbor = getGreyFromRGB(img.getRGB(j+1,i+2));
                        img.setRGB(j+1,i+2, getGSRGBfromGrey(neighbor + (error*2))); //error * 2
                    }
                    if(j < width-2){ //there are two columns to the right
                        neighbor = getGreyFromRGB(img.getRGB(j+2,i+2));
                        img.setRGB(j+2,i+2, getGSRGBfromGrey(neighbor + error)); //error * 1
                    }

                    if(j > 0){ //there is a column to left
                        neighbor = getGreyFromRGB(img.getRGB(j-1,i+2));
                        img.setRGB(j-1,i+2, getGSRGBfromGrey(neighbor + (error*2))); //error * 2
                    }
                    if(j > 1){ //there are two columns to the left
                        neighbor = getGreyFromRGB(img.getRGB(j-2,i+2));
                        img.setRGB(j-2,i+2, getGSRGBfromGrey(neighbor + error)); //error * 1
                    }
                }

                output.setRGB(j,i,newColor);
            }
        }
        try {
            // ImageIO.write(output, filename.replaceAll(".*[.]", ""), new File(filename.replaceAll("[.].*", "") + "_dithered." + "png"));
            ImageIO.write(output, filename.replaceAll(".*[.]", ""), new File(filename.replaceAll("[.].*", "") + "_dithered." + filename.replaceAll(".*[.]", "")));
        } catch (Exception ex) {
            System.out.println("Image write failed.");
            ex.printStackTrace();
        }
    }

    //Returns a greyscale value based on luminosity from a given RGB value
    // The greyscale value of a color can be calculated using 0.21R + 0.72G + 0.07B based on luminosity
    // https://www.johndcook.com/blog/2009/08/24/algorithms-convert-color-grayscale/
    public static int getGreyFromRGB(int rgb){
        int red = (rgb&0xff0000)>>16;
        int green = (rgb&0xff00)>>8;
        int blue = rgb&0xff;
        int greyValue = (int)(0.21*red + 0.72*green + 0.07*blue);
        // greyValue = (int)((red+green+blue)/3); //Average of RGB for grey
        return greyValue;
    }

    // Returns a Greyscale RGB equivalent of a greyscale value between 0-255
    // In TYPE_INT_ARGB format
    public static int getGSRGBfromGrey(int greyscale){
        return (0xff<<24)+(greyscale<<16)+(greyscale<<8)+greyscale;
    }
    public static int getGSRGBfromGrey(double greyscale){
        return getGSRGBfromGrey((int)greyscale);
    }
 }
