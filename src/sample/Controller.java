package sample;


import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.ComboBoxListCell;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller extends Window {
    @FXML
    private TextField dirPath;

    @FXML
    private ListView menuList;

    @FXML
    private TextField outputPath;

    @FXML
    private Text whereFileWasWritten;

    public static final ObservableList menus = FXCollections.observableArrayList();
    private static ArrayList<String> keys = new ArrayList<>();

    static ArrayList<String> selectedMenus = new ArrayList<>();
    static ArrayList<File> output = new ArrayList<>();


    public void openButton(ActionEvent event) {
        menus.clear();
        menuList.setItems(null);
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory =
                directoryChooser.showDialog(this);

        if (selectedDirectory == null) {
            dirPath.setText("No Directory selected");
            outputPath.setText("No Directory selected");
        } else {
            dirPath.setText(selectedDirectory.getAbsolutePath());
            outputPath.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public void outputButton(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory =
                directoryChooser.showDialog(this);

        if (selectedDirectory == null) {
            outputPath.setText("No Directory selected");
        } else {
            outputPath.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public void generateMenu(ActionEvent event) {
        ArrayList<String> dataFromFiles = readSelectedFiles();
        ArrayList<String> listOfFoodsNeeded = filterFoods(dataFromFiles);
        HashMap<String, Integer> mappedFoods = fillMap(listOfFoodsNeeded);
        if (selectedMenus.isEmpty() || menus.isEmpty()) {
            whereFileWasWritten.setText("No menus were selected/found");
        } else {
            writeToFile(mappedFoods);
        }
    }

    private void writeToFile(HashMap<String, Integer> mappedFoods) {
        String date = getTodaysDate();
        String fileName = outputPath.getText() + "\\" + "erikOrguShoppingList" + date + ".txt";
        File f = new File(fileName);
        Integer copy = 1;
        while (f.exists() && !f.isDirectory()) {
            fileName = outputPath.getText() + "\\" + "erikOrguShoppingList" + date + "(" + copy + ").txt";
            f = new File(fileName);
            copy++;
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(fileName, "UTF-8");
            for (int key = 0; key < keys.size(); key++) {
                writer.println(mappedFoods.get(keys.get(key)) + keys.get(key));
            }
            whereFileWasWritten.setText("Shopping list was written to this location: \n " + fileName);
            writer.close();
        } catch (FileNotFoundException e) {
            whereFileWasWritten.setText("Invalid output location.");
        } catch (UnsupportedEncodingException e) {
            System.out.println("Mittesobilik enkodeering!");
        }


    }

    public static String getTodaysDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate localDate = LocalDate.now();
        return dtf.format(localDate);
    }

    private static HashMap<String, Integer> fillMap(ArrayList<String> listOfFoodsNeeded) {
        keys.clear();
        HashMap<String, Integer> mappedFoods = new HashMap<>();
        for (int food = 0; food < listOfFoodsNeeded.size(); food++) {
            String[] splitValues = listOfFoodsNeeded.get(food).split(" ");
            String foodValue = splitValues[0];
            Integer foodWeight = 0;
            if (foodValue.contains("g")) {
                foodWeight = Integer.parseInt(foodValue.substring(0, foodValue.indexOf("g")));
            } else if (foodValue.contains("ml")) {
                foodWeight = Integer.parseInt(foodValue.substring(0, foodValue.indexOf("m")));
            }
            String key = formulateKey(splitValues);
            if (mappedFoods.containsKey(key)) {
                mappedFoods.replace(key, mappedFoods.get(key) + foodWeight);
            } else {
                mappedFoods.put(key, foodWeight);
                keys.add(key);
            }
        }
        return mappedFoods;
    }

    private static String formulateKey(String[] splitValues) {
        String key = "";
        if (splitValues[0].contains("ml")) {
            key = key.concat("ml ");
        } else {
            key = key.concat("g ");
        }
        if (splitValues.length > 2) {
            for (int values = 1; values < splitValues.length; values++) {
                key = key.concat(splitValues[values] + " ");
            }
        } else {
            key = key.concat(splitValues[1]);
        }
        return key;
    }


    private static ArrayList<String> filterFoods(ArrayList<String> output) {
        Pattern pattern = Pattern.compile("[0-9]+[ml|g]+[ ].*");
        ArrayList<String> listOfFoodsNeeded = new ArrayList<>();
        for (int out = 0; out < output.size(); out++) {
            Matcher match = pattern.matcher(output.get(out));
            if (match.find()) {
                listOfFoodsNeeded.add(match.group(0));
            }

        }
        return listOfFoodsNeeded;
    }

    private ArrayList<String> readSelectedFiles() {
        ArrayList<String> linesOfMenus = new ArrayList<>();
        ArrayList<File> checkedMenus = generateCheckedMenuPaths();
        for (File menu : checkedMenus) {
            PDDocument doc = null;
            try {
                doc = PDDocument.load(menu);
                PDFTextStripper pdfStripper = new PDFTextStripper();
                String[] output = pdfStripper.getText(doc).split("\n");
                for (int outputted = 0; outputted < output.length; outputted++) {
                    linesOfMenus.add(output[outputted]);
                }
                doc.close();
            } catch (IOException e) {
                System.out.println("Document not found");
            }
        }
        return linesOfMenus;

    }

    private ArrayList<File> generateCheckedMenuPaths() {
        ArrayList<File> returnedList = new ArrayList<>();
        for (File check : output) {
            if (selectedMenus.contains(check.getName())) returnedList.add(check);

        }
        return returnedList;
    }

    @FXML
    public void initialize() {
        dirPath.textProperty().addListener(((observable, oldValue, newValue) -> {
            outputPath.setText(newValue);
            menuList.setItems(null);
            selectedMenus.clear();
            menus.clear();
            //TODO: bulletproof this
            File folder = new File(newValue);
            try {
                output = findAllErikOrguMenus(folder);
                if (!output.isEmpty() || output == null) {
                    whereFileWasWritten.setText("Menus found, check the ones you want to get the shopping list for");

                    for (File put : output) {
                        menus.add(put.getName());
                    }
                    menuList.setItems(menus);
                    menuList.setCellFactory(CheckBoxListCell.forListView(new Callback<String, ObservableValue<Boolean>>() {
                        @Override
                        public ObservableValue<Boolean> call(String item) {
                            BooleanProperty observable = new SimpleBooleanProperty();
                            observable.addListener((obs, wasSelected, isNowSelected) -> {
                                        if (isNowSelected) {
                                            if (!selectedMenus.contains(item)) selectedMenus.add(item);
                                        } else {
                                            if (selectedMenus.contains(item)) selectedMenus.remove(item);
                                        }
                                    }

                            );
                            return observable;
                        }
                    }));
                } else {
                    whereFileWasWritten.setText("No menus were found, try to search from another folder");
                }
            } catch (IOException e) {
                //TODO: Error message
                System.out.println("Nothing found!");
            }
        }));
        outputPath.textProperty().addListener((observable, oldValue, newValue) -> {
            outputPath.setText(newValue);
        });


    }

    private static ArrayList<File> findAllErikOrguMenus(File folder) throws IOException {
        File[] listOfFiles = folder.listFiles();
        ArrayList<File> listOfMenus = new ArrayList<>();
        if (!(listOfFiles == null)) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if (listOfFiles[i].getName().contains(".pdf") && listOfFiles[i].getName().contains("Your_menu_")) {
                        listOfMenus.add(listOfFiles[i]);
                    }
                }
            }
        }
        return listOfMenus;
    }


}
