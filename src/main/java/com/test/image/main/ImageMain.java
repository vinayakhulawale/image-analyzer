/**
 * 
 */
package com.test.image.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class ImageMain {

    private static int medianSize;

    private static int[] wMedian;

    private static int numberOfBuckets;

    private static double[] rgbScale;

    private static String imgDir;

    private static double[] wLinear;

    private static double fractionOfSaltAndPepper;

    private static int linearSize;

    private static int gaussianMean;

    private static int gaussianStd;
    
    private static String toString(long nanoSecs) {
        int minutes    = (int) (nanoSecs / 60000000000.0);
        int seconds    = (int) (nanoSecs / 1000000000.0)  - (minutes * 60);
        int millisecs  = (int) ( ((nanoSecs / 1000000000.0) - (seconds + minutes * 60)) * 1000);


        if (minutes == 0 && seconds == 0)
           return millisecs + "ms";
        else if (minutes == 0 && millisecs == 0)
           return seconds + "s";
        else if (seconds == 0 && millisecs == 0)
           return minutes + "min";
        else if (minutes == 0)
           return seconds + "s " + millisecs + "ms";
        else if (seconds == 0)
           return minutes + "min " + millisecs + "ms";
        else if (millisecs == 0)
           return minutes + "min " + seconds + "s";

        return minutes + "min " + seconds + "s " + millisecs + "ms";
     }

    private static void initializeConfig() {
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = ClassLoader.getSystemResourceAsStream(("config.properties"));
            prop.load(input);
            prop.forEach((k, v) -> System.out.println("key: " + k + " value:" + v));

            medianSize = getIntProperty(prop, "medianSize");
            wMedian = getIntArrayProperty(prop, "wMedian");
            numberOfBuckets = getIntProperty(prop, "numberOfBuckets");
            imgDir = getStringProperty(prop, "directory");
            linearSize = getIntProperty(prop, "linearSize");
            gaussianMean = getIntProperty(prop, "gaussianMean");
            gaussianStd = getIntProperty(prop, "gaussianStd");
            fractionOfSaltAndPepper = getDoubleProperty(prop, "fractionOfSaltAndPepper");
            rgbScale = getDoubleArrayProperty(prop, "rgbScale");
            wLinear = getDoubleArrayProperty(prop, "wLinear");

        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static double getDoubleProperty(Properties prop, String property) {
        return null != prop.getProperty(property) ? Double.parseDouble(prop.getProperty(property)) : 0;
    }

    private static int getIntProperty(Properties prop, String property) {
        return null != prop.getProperty(property) ? Integer.parseInt(prop.getProperty(property)) : 0;
    }

    private static String getStringProperty(Properties prop, String property) {
        return null != prop.getProperty(property) ? prop.getProperty(property) : "";
    }

    private static int[] getIntArrayProperty(Properties prop, String property) {
        String strProperty = prop.getProperty(property);
        if (null != strProperty) {

            return Arrays.stream(strProperty.split(",")).map(String::trim).mapToInt(Integer::parseInt).toArray();
        }
        return null;
    }

    private static double[] getDoubleArrayProperty(Properties prop, String property) {
        String strProperty = prop.getProperty(property);
        if (null != strProperty) {

            return Arrays.stream(strProperty.split(",")).map(String::trim).mapToDouble(Double::parseDouble).toArray();
        }
        return null;
    }

    private static String createOutputDirectory() {
        String path = "output_"; // + new Random(6877989l).nextInt();
        try {
            Files.createDirectories(Paths.get(path));
            return path;
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    private static BufferedImage getImageFromArray(int[][] pixels, int width, int height) {
        BufferedImage image = new BufferedImage(width, height,     BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                image.setRGB(col, row, pixels[row][col]);
            }
         }
        return image;
    }
    
    private static void toGrayScale(String outputDir, Path file, double[] rgbScale ) throws IOException {
        
        BufferedImage image = ImageIO.read(file.toFile());
        long startTime = System.nanoTime();
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];

        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
              int pixel = image.getRGB(col, row);
             //get alpha
              int a = (pixel>>24) & 0xff;
             //get red
              int r = (pixel>>16) & 0xff ;
              //System.out.println("result[row][col] RED = "+ r );
              r = (int) (r * rgbScale[0]);
              //System.out.println("result[row][col] RED = "+ r);
              //get green
              int g = (pixel>>8) & 0xff;
              //System.out.println("result[row][col] GREEN = "+ g );
              g = (int) (g * rgbScale[1]);
              //System.out.println("result[row][col] GREEN = "+ g );
              //get blue
              int b = pixel & 0xff;
              //System.out.println("result[row][col] Blue = "+ b );
              b = (int) (b * rgbScale[2]);
              //System.out.println("result[row][col] Blue = "+ b );
              int avg = (r+g+b)/3;
              result[row][col] = (a<<24) | (avg<<16) | (avg<<8) | avg;
              //System.out.println("result[row][col]  = "+ result[row][col] );
           }
        }

        BufferedImage newImage = getImageFromArray(result,width, height);
        Files.createDirectories(Paths.get(outputDir + "/GreyScaleAvg/"));
        File f = new File(outputDir + "/GreyScaleAvg/"+ file.getFileName().toString());
        ImageIO.write(newImage, "png", f);
       
        long endTime = System.nanoTime();
        System.out.println("GrayScale - Time taken for "+ file.getFileName().toString() + " is " + toString(endTime - startTime));
        
    }
    
    private static void toSaltPaperFraction(String outputDirectory, Path file, double fractionOfSaltAndPepper) throws IOException {
       
        BufferedImage image = ImageIO.read(file.toFile());
        long startTime = System.nanoTime();
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];

        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
                  if ( new Random().nextDouble() < fractionOfSaltAndPepper) {
                      if ( new Random().nextDouble() < 0.5) {
                          result[row][col]   = 255;
                      }else {
                          result[row][col]   = 0;
                      }
                          
                  }else {
                      int pixel = image.getRGB(col, row);
                      //get alpha
                        int a = (pixel>>24) & 0xff;
                       //get red
                        int r = (pixel>>16) & 0xff ;
                        r = (int) (r * rgbScale[0]);
                        //get green
                        int g = (pixel>>8) & 0xff;
                        g = (int) (g * rgbScale[1]);
                        //get blue
                        int b = pixel & 0xff;
                        b = (int) (b * rgbScale[2]);
                        int avg = (r+g+b)/3;
                        result[row][col] = (a<<24) | (avg<<16) | (avg<<8) | avg;
                  }
           }
        }

        BufferedImage newImage = getImageFromArray(result,width, height);
        Files.createDirectories(Paths.get(outputDirectory + "/SaltnPepper/"));
        File f = new File(outputDirectory + "/SaltnPepper/"+ file.getFileName().toString());
        ImageIO.write(newImage, "png", f);
       
        long endTime = System.nanoTime();
        System.out.println("Salt n pepper - Time taken for "+ file.getFileName().toString() + " is " + toString(endTime - startTime));
        
        
    }
    
    private static int nextGaussianInt(Random r, int mean, int deviation) {
        int g = (int) ((int) mean + r.nextGaussian()*deviation);
        System.out.println("g = "+ g);
        return g;
    }
    
    private static void toGaussianNoise(String outputDirectory, Path file, int gaussianMean, int gaussianStd) throws IOException {
        // TODO Auto-generated method stub
        BufferedImage image = ImageIO.read(file.toFile());
        long startTime = System.nanoTime();
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];
        Random random = new Random();
        for (int row = 0; row < height; row++) {
           for (int col = 0; col < width; col++) {
              int pixel = image.getRGB(col, row);
            //get alpha
              int a = (pixel>>24) & 0xff;
             //get red
              int r = (pixel>>16) & 0xff ;
              r = (int) (r * rgbScale[0]);
              //get green
              int g = (pixel>>8) & 0xff;
              g = (int) (g * rgbScale[1]);
              //get blue
              int b = pixel & 0xff;
              b = (int) (b * rgbScale[2]);
              int avg = (r+g+b)/3 + nextGaussianInt(random, gaussianMean, gaussianStd) ;
              if  (avg< 0) {
                  avg = 0;
              }else if (avg > 255) {
                  avg = 255;
              }
              result[row][col] = (a<<24) | (avg<<16) | (avg<<8) | avg;
            
           }
        }
        
        BufferedImage newImage = getImageFromArray(result,width, height);
        Files.createDirectories(Paths.get(outputDirectory + "/GaussianNoise/"));
        File f = new File(outputDirectory + "/GaussianNoise/"+ file.getFileName().toString());
        ImageIO.write(newImage, "png", f);
       
        long endTime = System.nanoTime();
        System.out.println("GaussianNoise - Time taken for "+ file.getFileName().toString() + " is " + toString(endTime - startTime));
     
    }


    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        initializeConfig();

        String outputDirectory = createOutputDirectory();

        if (outputDirectory != null) {
            System.out.println(" Directory " + outputDirectory + " created ");
        }
        else {
            System.err.println(" could not create directory ");
        }

        try (Stream<Path> paths = Files.walk(Paths.get(imgDir))) {
            paths.filter(Files::isRegularFile).forEach(file -> {
                try {
                    toGrayScale(outputDirectory, file, rgbScale);
                    toSaltPaperFraction(outputDirectory, file, fractionOfSaltAndPepper);
                    toGaussianNoise(outputDirectory, file, gaussianMean, gaussianStd);
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            });
        }

    }

    
   

   

}
