package st.project.view;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public class SpriteSheet {
    private final BufferedImage[][] frames;
    private final BufferedImage[][] framesFlipped;
    private final int cols;
    private final int rows;

    public SpriteSheet(String resourcePath, int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        BufferedImage[][] loaded = null;
        BufferedImage[][] flipped = null;
        try {
            InputStream is = SpriteSheet.class.getResourceAsStream(resourcePath);
            if (is != null) {
                BufferedImage sheet = ImageIO.read(is);
                int fw = sheet.getWidth() / cols;
                int fh = sheet.getHeight() / rows;
                loaded = new BufferedImage[rows][cols];
                flipped = new BufferedImage[rows][cols];
                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < cols; col++) {
                        loaded[row][col] = sheet.getSubimage(col * fw, row * fh, fw, fh);
                        flipped[row][col] = createFlipped(loaded[row][col]);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        this.frames = loaded;
        this.framesFlipped = flipped;
    }

    private BufferedImage createFlipped(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.drawImage(src, w, 0, -w, h, null);
        g2.dispose();
        return out;
    }

    public boolean isLoaded() { return frames != null; }

    public BufferedImage getFrame(int row, int col) {
        if (!isLoaded()) return null;
        return frames[row % rows][col % cols];
    }

    public BufferedImage getFrameFlipped(int row, int col) {
        if (!isLoaded()) return null;
        return framesFlipped[row % rows][col % cols];
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
}
