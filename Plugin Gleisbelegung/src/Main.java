/*
@author: Manuel Serret
@email: manuel-serret@t-online.de
@contact: Email, Github, STS-Forum

Hinweis: In jeder Klasse werden alle Klassenvariablen erklärt, sowie jede Methode. Solltest du neue Variablen oder Methoden hinzufügen, vergiss bitte nicht sie zu implementieren.

In dieser Klasse werden viele Variablen gespeichert, da jede Klasse diese Klasse extendet.
Hier befindet sich auch die Hauptschleife des Plugins.
 */

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class Main extends Application implements Runnable{
    static long currentGameTime;                            //Speichert die aktuelle Zeit in Milisekunden, heißt 1 Minute entspricht 1000*60
    static boolean lastUpdateSuccessful;                    //Aktuall keine Verwendung

    static ArrayList<Zug> zuege;                            //Hier werden alle Zug-Objekte gespeichert
    static String[] bahnsteige;                             //Speichert die Namen aller Bahnsteige in einem Array
    static boolean[] bahnsteigeSichtbar;                    //Speichert ob die jeweiligen Bahnsteige sichtbar sind oder nicht (true = sichtbar; fals = versteckt)

    static Stage primaryStage;                              //Ist das Objekt für das aktuelle Fenster

    static int settingsUpdateInterwall = 15;                //Wie oft das Fenster (die Tabelle) aktualisiert wird.      (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static int settingsVorschau = 60;                       //Wie viele Zeilen die Tabelle hat.                         (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static int settingsGridWidth = 100;                     //Wie breit die einzelnen Spalten sind.                     (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static int settingsFontSize = 18;                       //Wie groß die Schriftgröße ist.                            (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static boolean settingsPlaySound = true;                //Soll bei einer Mehrfahcbelegung ein Ton abgespielt werden.(Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static boolean settingsShowInformations = true;         //Zeige die Informationenpanel.                             (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)
    static boolean settingsDebug = false;                   //Sollen zusätzliche Informationen geschrieben werden.      (Wird bei vorhandenen Einstellungen durch den dortigen Wert überschrieben)

    static long spielStart = System.currentTimeMillis();    //Zu welcher Uhrzeit das Spiel gestartet wurde.             (Wird auch bei automatischem Neustart neu gesetzt)
    private long lastRefresh = System.currentTimeMillis();  //Zu welcher Uhrzeit das Plugin zum letzten mal aktualisiert wurde
    static Pane fehlerMeldungen;                            //Panel für Fehlermeldungen auf der rechten Seite.          (Wird bald entfernt)
    static Pane informations;                               //Panel für alle Zuginformation                             (Zugnummer, Verspätung etc.)
    static PrintWriter logFile;                             //
    static Pane pZugSuche;                                  //Panel mit der Zugsuche
    static int errorCounter = 0;                            //Zählt alle auftreten Fehler für einen eventuellen automatischen Neustart
    static int maxErrorCounter = 10;                        //Ist der obrige Wert größer als dieser, wird das Plugin automatisch neu gestartet.
    static double stageWidth = 1000;                        //Standartmäßige Fenster-Breite                             (Wir bei Veränderung der Breite aktualisiert)
    static double stageHeight = 500;                        //Standartmäßige Fenster-Höhe                               (Wir bei Veränderung der Höhe aktualisiert)

    private Verbindung v;                                   //Objekt der Verbindungs-Klasse                             (Übernimmt Kommunikation mit der Schnittstelle)
    private Update u;                                       //Objekt der Update-Klasse                                  (Lässte ein Fenster erscheinen, sobald eine neuere Version verfügbar ist)
    private Fenster f;                                      //Objekt der Fenster-Klasse                                 (Kümmert sich um die Aktualisierung des UI)

    private String host;                                    //Die Ip des Rechnsers, auf welchem die Sim läuft           (Wird bei einer Änderung beim Pluginstart aktualisiert)
    private int version = 7;                                //Aktualle Version des Plugins
    private static AudioClip audio;
    private Socket socket;

    //Standartmäßige Methode bei Java Fx-Apps
    public static void main(String[] args) {
        launch(args);
    }

    //Erste Aufgerufene Methode (Ip-Abfrage, Updates-checken, Fenster erzeugen)
    @Override
    public void start(Stage primaryStage) throws Exception{
        u = new Update();
        u.checkForNewVersion(version);

        socket = new Socket(host, 3691);

        fehlerMeldungen = new Pane();
        audio = new AudioClip(getClass().getResource("Train_Horn.wav").toURI().toString());
        Main.primaryStage = primaryStage;

        int sceneWidth = 1000;
        int sceneHeight = 500;

        Label lHost = new Label("Bitte die IP des Rechners eingeben: ");
        lHost.setTooltip(new Tooltip("Nur ändern wenn der Simulator nicht auf diesem Rechner läuft!"));
        lHost.setStyle("-fx-text-fill: white;");
        lHost.setFont(Font.font(settingsFontSize));
        lHost.setTranslateX(sceneWidth/2 - 300);
        lHost.setTranslateY(sceneHeight/2 - 45);

        TextField tfHost = new TextField("localhost");
        tfHost.setStyle("-fx-text-fill: black;");
        tfHost.setFont(Font.font(settingsFontSize));
        tfHost.setTranslateX(sceneWidth/2);
        tfHost.setTranslateY(sceneHeight/2 - 50);

        Button btLoad = new Button("Verbinden");
        btLoad.setStyle("-fx-text-fill: black;");
        btLoad.setFont(Font.font(settingsFontSize));
        btLoad.setTranslateX(sceneWidth/2 - 75);
        btLoad.setTranslateY(sceneHeight/2);
        btLoad.setOnAction(e -> {
            startLoading(tfHost.getText());
        });

        Pane p = new Pane();
        p.setStyle("-fx-background-color: #303030");
        p.getChildren().addAll(lHost, tfHost, btLoad);

        Scene s = new Scene(p, sceneWidth, sceneHeight);

        Main.primaryStage.setScene(s);
        Main.primaryStage.setTitle("Plugin: Gleisbelegung");
        Main.primaryStage.show();
    }

    //Schreibt DEBUG-Informationen auf dei Konsole (nur wenn settingsDebug = true)
    public static void debugMessage(String message, boolean newLine){
        if(settingsDebug && newLine) System.out.println(message);
        else if(settingsDebug) System.out.print(message);
    }

    //Setzt das Loading.... Beim ersten Erzeugen der Tabelle
    public void setLoadingScene(){
        Label l = new Label("Bereite vor...");
        l.setStyle("-fx-text-fill: white;");
        l.setFont(Font.font(settingsFontSize));
        l.setTranslateX(primaryStage.getWidth()/2);
        l.setTranslateY(primaryStage.getHeight()/2);

        Pane p = new Pane();
        p.setStyle("-fx-background-color: #303030");
        p.getChildren().addAll(l);

        Scene s = new Scene(p, stageWidth+1, stageHeight);

        primaryStage.setScene(s);
    }

    //Startet die Verbindung zur Schnitstelle
    public void startLoading(String host) {
        this.host = host;

        setLoadingScene();

        readSettings();
        if(v == null){
            v = new Verbindung(socket);
        } else{
            zuege = new ArrayList<>();
        }

        f = new Fenster();
        v.update();
        f.update();
        f.setGridScene();

        Thread t = new Thread(this, "App-Schleife");
        t.setDaemon(true);
        t.start();
    }

    //Liest die Vorhanden Einstellungen und überschreibt die Standart-Werte
    private void readSettings(){
        try {
            File f = File.createTempFile("temp", ".txt");
            String filePath = f.getAbsolutePath().replace(f.getName(), "");
            f.delete();

            BufferedReader br = new BufferedReader(new FileReader(filePath + "Plugin_Gleisbelegung_Settings.txt"));

            settingsUpdateInterwall = Integer.parseInt(br.readLine());
            settingsVorschau = Integer.parseInt(br.readLine());
            settingsGridWidth = Integer.parseInt(br.readLine());
            settingsFontSize = Integer.parseInt(br.readLine());
            settingsShowInformations = Boolean.parseBoolean(br.readLine());
            settingsPlaySound = Boolean.parseBoolean(br.readLine());
            settingsDebug = Boolean.parseBoolean(br.readLine());
        } catch (Exception e) {
            System.out.println("Settings file not found!");
        }
    }

    //Hier werden Fehlermeldungen in ein neues Fenster eingetragen (wie z.B. beim automatischen Neustart)
    public static void errorWindow(int exitPoint, String fehlermeldung){
        Runnable r = () -> {
            Platform.runLater(() -> {
                Stage error = new Stage();
                Scene s;
                Pane p;
                Button b;
                Label lTitle;
                Label lText;

                lTitle = new Label("Es ist ein unerwarteter Fehler aufgetreten! (Status " + exitPoint + ")");
                lTitle.setFont(Font.font(settingsFontSize));
                lTitle.setStyle("-fx-text-fill: red;");
                lTitle.setTranslateY(10);
                lTitle.setTranslateX(10);

                lText = new Label(fehlermeldung);
                lText.setFont(Font.font(settingsFontSize));
                lText.setStyle("-fx-text-fill: white;");
                lText.setTranslateY(60);
                lText.setTranslateX(10);
                lText.setPrefWidth(primaryStage.getWidth()/2-20);
                lText.setWrapText(true);

                b = new Button("Ok");
                b.setFont(Font.font(settingsFontSize));
                b.setTranslateX(primaryStage.getWidth()/4-20);
                b.setTranslateY(primaryStage.getHeight()/2-100);
                b.setOnAction(e -> error.close());

                p = new Pane(lTitle, lText, b);
                p.setStyle("-fx-background: #303030");

                s = new Scene(p);
                p.setStyle("-fx-background: #303030");

                error.setTitle("Ferhlermeldung");
                error.setWidth(primaryStage.getWidth()/2);
                error.setHeight(primaryStage.getHeight()/2);
                error.setScene(s);
                error.setAlwaysOnTop(true);
                error.show();
            });
        };
        new Thread(r).start();
    }

    //Der Sound, der der Gespielt wird, wenn eine Mehrachbelegung entsteht
    public static void playColisonSound(){
        if(settingsPlaySound){
            try{
                audio.setVolume(0.04);
                audio.play();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    //Schreibt Fehlermeldungen auf das Panel fehlerMeldungen
    public static void addMessageToErrorPane(String message){
        Label l = new Label(message);
        l.setFont(Font.font(settingsFontSize-5));
        l.setStyle("-fx-text-fill: white;");
        l.setTranslateY(fehlerMeldungen.getChildren().size()*20);

        if(fehlerMeldungen.getChildren().size() >= 10){
            Platform.runLater(() -> {
                fehlerMeldungen.getChildren().remove(0);
                fehlerMeldungen.getChildren().add(l);

                for(int i = 0; i < fehlerMeldungen.getChildren().size(); i++){
                    fehlerMeldungen.getChildren().get(i).setTranslateY(i*20);
                }
            });
        } else{
            Platform.runLater(() -> fehlerMeldungen.getChildren().add(l));
        }
    }

    //Hauptschleife des Plugins
    @Override
    public void run() {
        long timeFromLastUpdate = 0;

        while(errorCounter < maxErrorCounter){
            try {
                int time = (int) ((System.currentTimeMillis() - timeFromLastUpdate) / 1000);

                if(time >= settingsUpdateInterwall){
                    timeFromLastUpdate = System.currentTimeMillis();

                    Runnable r = () -> {
                        v.update();
                        f.update();
                    };
                    new Thread(r, "Aktualisiere Verbindung und Fenster").start();
                }

                f.updateSimTime(settingsUpdateInterwall - time);

                if(currentGameTime % (1000*60) >= 0 && currentGameTime % (1000*60) <= 1000){
                    Runnable r = () -> {
                        debugMessage("INFORMATION: Aktualisiere Tabelle...", false);
                        f.refreshGrid();
                        debugMessage("Beendet!", true);
                        debugMessage("INFORMATION: Du benutzt das Plugin seit " + ((System.currentTimeMillis()-spielStart)/(1000*60)) + " Minute(n)!", true);
                    };
                    new Thread(r, "Aktualisiere Tabelle").start();
                }

                if((System.currentTimeMillis() - lastRefresh) % (1000*60*45) < 1000){
                    lastRefresh = System.currentTimeMillis()-1000;
                    break;
                }

                currentGameTime += 1000;
                Thread.sleep(1000);
            } catch (Exception e) {
                debugMessage("FEHLER: Bitte folgenden Code beachten:", true);
                e.printStackTrace();
            }
        }

        errorCounter = 0;
        System.out.println("\n\n\n\n\n**********************************************************************************\n\t\t\t\t\t\tNeustart\n**********************************************************************************");
        Platform.runLater(() -> errorWindow(0, "Das Plugin wurde aufgrund einiger Fehler neu gestartet!"));
        Platform.runLater(() -> startLoading(host));
    }
}