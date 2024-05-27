import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class TerrainGenerator extends JFrame {
    private DiamondSquare ds;
    private TerrainVisualizer visualizer;
    private int size = 513; // Размер карты (должен быть 2^n + 1)
    private double roughness = 1.0; // Параметр R

    public TerrainGenerator() {
        setTitle("Diamond-Square Terrain Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 800);

        ds = new DiamondSquare(size, roughness);
        visualizer = new TerrainVisualizer(ds.getMap(), 1);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 2));

        JLabel scaleLabel = new JLabel("Scale:");
        JSlider scaleSlider = new JSlider(1, 10, 1);
        scaleSlider.addChangeListener(e -> visualizer.setScale(scaleSlider.getValue()));

        JLabel roughnessLabel = new JLabel("Roughness:");
        JTextField roughnessField = new JTextField(String.valueOf(roughness));

        roughnessField.addActionListener(e -> {
            try {
                double newRoughness = Double.parseDouble(roughnessField.getText());
                if (newRoughness >= 0.1 && newRoughness <= 10.0) {
                    roughness = newRoughness;
                    ds.generateTerrain(roughness);
                    visualizer.setMap(ds.getMap());
                } else {
                    JOptionPane.showMessageDialog(this, "Roughness value must be between 0.1 and 10.0.");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid roughness value. Please enter a number.");
            }
        });

        controlPanel.add(scaleLabel);
        controlPanel.add(scaleSlider);
        controlPanel.add(roughnessLabel);
        controlPanel.add(roughnessField);

        add(visualizer, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                visualizer.startDrag(e.getPoint());
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                visualizer.drag(e.getPoint());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TerrainGenerator frame = new TerrainGenerator();
            frame.setVisible(true);
        });
    }
}

class DiamondSquare {
    private int size;
    private double[][] map;
    private Random random;

    public DiamondSquare(int size, double roughness) {
        this.size = size;
        this.map = new double[size][size];
        this.random = new Random();
        generateTerrain(roughness);
    }

    public void generateTerrain(double roughness) {
        map[0][0] = random.nextDouble();
        map[0][size - 1] = random.nextDouble();
        map[size - 1][0] = random.nextDouble();
        map[size - 1][size - 1] = random.nextDouble();

        for (int stepSize = size - 1; stepSize > 1; stepSize /= 2) {
            // Diamond step
            for (int x = 0; x < size - 1; x += stepSize) {
                for (int y = 0; y < size - 1; y += stepSize) {
                    double avg = (map[x][y] + map[x + stepSize][y] + map[x][y + stepSize] + map[x + stepSize][y + stepSize]) / 4.0;
                    map[x + stepSize / 2][y + stepSize / 2] = avg + (random.nextDouble() * 2 - 1) * roughness;
                }
            }

            // Square step
            for (int x = 0; x < size; x += stepSize / 2) {
                for (int y = (x + stepSize / 2) % stepSize; y < size; y += stepSize) {
                    double sum = 0;
                    int count = 0;
                    if (x - stepSize / 2 >= 0) {
                        sum += map[x - stepSize / 2][y];
                        count++;
                    }
                    if (x + stepSize / 2 < size) {
                        sum += map[x + stepSize / 2][y];
                        count++;
                    }
                    if (y - stepSize / 2 >= 0) {
                        sum += map[x][y - stepSize / 2];
                        count++;
                    }
                    if (y + stepSize / 2 < size) {
                        sum += map[x][y + stepSize / 2];
                        count++;
                    }
                    double avg = sum / count;
                    map[x][y] = avg + (random.nextDouble() * 2 - 1) * roughness;
                }
            }

            roughness /= 2.0;
        }
    }

    public double[][] getMap() {
        return map;
    }
}

class TerrainVisualizer extends JPanel {
    private double[][] map;
    private int scale;
    private Point dragStartPoint;
    private int xOffset, yOffset;

    public TerrainVisualizer(double[][] map, int scale) {
        this.map = map;
        this.scale = scale;
        this.xOffset = 0;
        this.yOffset = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage image = new BufferedImage(map.length, map.length, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < map.length; x++) {
            for (int y = 0; y < map.length; y++) {
                Color color = getColorForHeight(map[x][y]);
                image.setRGB(x, y, color.getRGB());
            }
        }

        g.drawImage(image, xOffset, yOffset, map.length * scale, map.length * scale, null);
    }

    private Color getColorForHeight(double height) {
        height = Math.max(0, Math.min(1, height)); // Гарантируем, что высота находится в диапазоне от 0 до 1

        if (height < 0.3) {
            int blue = (int) (height / 0.3 * 255);
            return new Color(0, 0, Math.min(255, Math.max(0, blue))); // Вода
        } else if (height < 0.6) {
            int green = (int) ((height - 0.3) / 0.3 * 255);
            return new Color(0, Math.min(255, Math.max(0, green)), 0); // Равнины
        } else {
            int brownRed = 120 + (int) ((height - 0.6) / 0.4 * 135); // Подбираем компоненты цвета для коричневого
            int brownGreen = 60 + (int) ((height - 0.6) / 0.4 * 70);
            int brownBlue = 20 + (int) ((height - 0.6) / 0.4 * 30);
            return new Color(Math.min(255, Math.max(0, brownRed)), Math.min(255, Math.max(0, brownGreen)), Math.min(255, Math.max(0, brownBlue))); // Горы (Коричневый цвет)
        }
    }



    public void setScale(int scale) {
        this.scale = scale;
        repaint();
    }

    public void setMap(double[][] map) {
        this.map = map;
        repaint();
    }

    public void startDrag(Point p) {
        dragStartPoint = p;
    }

    public void drag(Point p) {
        if (dragStartPoint != null) {
            xOffset += p.x - dragStartPoint.x;
            yOffset += p.y - dragStartPoint.y;
            dragStartPoint = p;
            repaint();
        }
    }
}
