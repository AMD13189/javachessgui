package javachessgui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

import javafx.stage.*;

import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

public class Game {
    
        public HBox clip_box=new HBox(2);
        public HBox save_pgn_box=new HBox(2);
    
        private TextField pgn_name_text = new TextField();
    
        private String pgn;
    
        String initial_dir="";
    
        ListView<String> list = new ListView<String>();
        
        TextArea pgn_text=new TextArea();
    
        public VBox vertical_box=new VBox(2);
        FileChooser f=new FileChooser();
        
        private Stage s=new Stage();
        
        private Board b;
        
        private TextArea game_text = new TextArea ();
        
        final private int MAX_MOVES=250;
        
        String[] pgn_lines=new String[MAX_MOVES];
        
        private String[] moves=new String[MAX_MOVES];
        public String initial_position;
        private String[] positions=new String[MAX_MOVES];
        
        int move_indices[]=new int[MAX_MOVES];
        int start_fen_end_index=0;
        
        private int move_ptr=0;
        
        private int game_ptr=0;
        
        public void reset(String initial_fen)
        {
            
            move_ptr=0;
            
            initial_position=initial_fen;
            
            update_game();
            
        }
        
        public void add_move(String san,String fen_after)
        {
            
            if(game_ptr<move_ptr)
            {
                move_ptr=game_ptr;
            }
            
            if(move_ptr>=MAX_MOVES)
            {
                
            }
            else
            {
                
                positions[move_ptr]=fen_after;
                moves[move_ptr++]=san;
                game_ptr++;
                
            }
            
            update_game();
            
        }
        
        public String to_begin()
        {
            game_ptr=0;
            update_game();
            return initial_position;
        }
        
        public String back()
        {
            
            if(game_ptr==0)
            {
                return initial_position;
            }
            else
            {
                game_ptr--;
                update_game();
                if(game_ptr==0)
                {
                    return initial_position;
                }
                return positions[game_ptr-1];
                
            }
            
        }
        
        public String forward()
        {
            
            if(game_ptr==move_ptr)
            {
                if(game_ptr==0)
                {
                    return initial_position;
                }
                return positions[game_ptr-1];
            }
            else
            {
                game_ptr++;
                update_game();
                return positions[game_ptr-1];
                
            }
            
        }
        
        public String to_end()
        {
            game_ptr=move_ptr;
            update_game();
            if(move_ptr==0)
            {
                return initial_position;
            }
            return positions[move_ptr-1];
        }
        
        public String delete_move()
        {
            
            if(game_ptr<move_ptr)
            {
                move_ptr=game_ptr;
            }
            
            if(move_ptr==0)
            {
                // nothing to delete
                return(initial_position);
            }
            else
            {
                if(move_ptr>0)
                {
                    move_ptr--;
                    game_ptr--;
                }
                
                update_game();
                
                if(move_ptr==0)
                {
                    return initial_position;
                }
                
                return(positions[move_ptr-1]);
            }
        }
        
        public void update_game()
        {
            
            String[] game_buffer=new String[MAX_MOVES+1];
            
            game_buffer[0]="*";
            
            for(int i=0;i<move_ptr;i++)
            {
                game_buffer[i+1]=moves[i];
            }

            //game_text.setText(game_buffer);
            
            ObservableList<String> items =FXCollections.observableArrayList(
                Arrays.copyOfRange(game_buffer, 0, move_ptr+1)
            );
        
            list.setItems(items);
            
            list.getSelectionModel().select(game_ptr);
            list.scrollTo(game_ptr);
            
            pgn_text.setText(calc_pgn());
            
            if(game_ptr>0)
            {
                
                pgn_text.positionCaret(move_indices[game_ptr-1]);
                pgn_text.selectNextWord();

            }
            else
            {
                pgn_text.selectRange(0, start_fen_end_index);
            }
                        
        }
        
        public String calc_pgn()
        {
            Board dummy=new Board(false);
            
            dummy.set_from_fen(initial_position);
            
            int fullmove_number=dummy.fullmove_number;
            int turn=dummy.turn;
            
            pgn="[StartFen \""+initial_position+"\"]\n";
            start_fen_end_index=pgn.length()-1;
            pgn+="[Flip \""+b.flip+"\"]\n";
            
            pgn+="\n";
            
            if(move_ptr>0)
            {
                pgn+=fullmove_number+". ";
                
                if(turn==Board.TURN_BLACK)
                {
                    pgn+="... ";
                }
                
                move_indices[0]=pgn.length();
                
                pgn+=moves[0]+" ";
            }
            
            for(int i=1;i<move_ptr;i++)
            {
                dummy.set_from_fen(positions[i-1]);
                turn=dummy.turn;
                if(dummy.turn==Board.TURN_WHITE)
                {
                    fullmove_number++;
                    pgn+=fullmove_number+". ";
                }
                move_indices[i]=pgn.length();
                pgn+=moves[i]+" ";
            }
            
            return pgn;
            
        }
        
        private void set_from_pgn_lines()
        {
            
            move_ptr=0;
            
            initial_position="rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
            
            int line_cnt=0;
            
            // read headers
            int empty_cnt=0;
            
            Boolean finished=false;
            
            do
            {
                String line=pgn_lines[line_cnt++];
                
                if(line_cnt<pgn_lines.length)
                {
                    if(line.length()<2)
                    {
                        finished=true;
                    }
                    else
                    {
                        if(line.charAt(0)!='[')
                        {
                            finished=true;
                        }
                        else
                        {
                            //System.out.println("header "+line);
                            Pattern get_flip = Pattern.compile("(Flip .true)");
                            Matcher flip_matcher = get_flip.matcher(line);
            
                            if(flip_matcher.find())
                            {
                                b.flip=true;
                            }
                            else
                            {
                                b.flip=false;
                            }
                        }
                    }
                }
                else
                {
                    finished=true;
                }
                
            }while(!finished);
            
            String body="";
            while(line_cnt<pgn_lines.length)
            {
                String line=pgn_lines[line_cnt++];
                if(line.length()<2)
                {
                    break;
                }
                body+=line+" ";
            }
            
            //System.out.println("body: "+body);
            
            MyTokenizer t=new MyTokenizer(body);
            
            String token;
            
            b.reset();
            
            while((token=t.get_token())!=null)
            {
                //System.out.println("token: "+token);
                
                if(b.is_san_move_legal(token))
                {
                    b.make_san_move(token, false);
                    String fen_after=b.report_fen();
                    add_move(token,fen_after);
                    
                    //System.out.println("san: "+token+" "+fen_after);
                }
                
            }
            
            game_ptr=move_ptr;
            
            b.drawBoard();
            
            update_game();
            
        }
        
        private EventHandler<MouseEvent> mouseHandler = new EventHandler<MouseEvent>() {
 
            @Override
            public void handle(MouseEvent mouseEvent) {

                int x=(int)mouseEvent.getX();
                int y=(int)mouseEvent.getY();
                
                String type=mouseEvent.getEventType().toString();

                if(type.equals("MOUSE_CLICKED"))
                {
                    //System.out.println("Mouse clicked over pgn text at x="+x+" y="+y);
                    
                    //System.out.println("caret position: "+pgn_text.getCaretPosition());
                    
                    int click_index=pgn_text.getCaretPosition();
                            
                    for(int i=0;i<move_ptr;i++)
                    {
                        int move_index=move_indices[i];
                        if(click_index<move_index)
                        {
                            game_ptr=i;
                            
                            String pos=initial_position;
                            if(game_ptr>0){pos=positions[game_ptr-1];}
                                                        
                            b.set_from_fen_inner(pos,false);
                            b.drawBoard();
                            
                            update_game();
                            
                            return;
                        }
                    }
                    
                    if(move_ptr>0)
                    {
                        game_ptr=move_ptr;
                            
                        String pos=positions[game_ptr-1];

                        b.set_from_fen_inner(pos,false);
                        b.drawBoard();

                        update_game();
                    }
                }

            }
        
        };
        
        private void look_for_initial_dir()
        {
            if(initial_dir.equals(""))
            {
                MyFile config=new MyFile("config.txt");
                String result=config.get_field("initial_dir");
                if(result!=null)
                {
                    initial_dir=result;
                }
            }
        }
        
        private Clipboard clip=Clipboard.getSystemClipboard();
        
        public Game(Stage set_s,Board set_b)
        {
            
            s=set_s;
            b=set_b;
            
            Button open_pgn_button=new Button();
            open_pgn_button.setText("Open PGN");
            
            open_pgn_button.setOnAction(new EventHandler<ActionEvent>() {
                    
                @Override public void handle(ActionEvent e) {
                    
                    look_for_initial_dir();
                    if(initial_dir!="")
                    {
                        File dir=new File(initial_dir);

                        f.setInitialDirectory(dir);
                    }
                                        
                    File file = f.showOpenDialog(s);
                    
                    if(file==null){return;}
                    
                    String path=file.getPath();
                    
                    pgn_name_text.setText(path);

                    initial_dir=path.substring(0,path.lastIndexOf(File.separator));

                    MyFile config=new MyFile("config.txt");
                    config.set_field("initial_dir",initial_dir);

                    MyFile my_file=new MyFile(path);

                    pgn_lines=my_file.read_lines();

                    set_from_pgn_lines();
                     
                    }
                
            });
            
            
            Button save_as_pgn_button=new Button();
            save_as_pgn_button.setText("Save as: ");
            
            save_as_pgn_button.setOnAction(new EventHandler<ActionEvent>() {
                    
                @Override public void handle(ActionEvent e) {
                    
                    String path=pgn_name_text.getText();
                    
                    if(path.length()>0)
                    {
                     
                        MyFile my_file=new MyFile(path);

                        calc_pgn();

                        my_file.content=pgn;

                        my_file.write_content();
                     
                    }
                }
                
            });
            
            Button save_to_pgn_button=new Button();
            save_to_pgn_button.setText("Save to PGN");
            
            save_to_pgn_button.setOnAction(new EventHandler<ActionEvent>() {
                    
                @Override public void handle(ActionEvent e) {
                    
                    look_for_initial_dir();
                    if(initial_dir!="")
                    {
                        File dir=new File(initial_dir);

                        f.setInitialDirectory(dir);
                    }
                                        
                     File file = f.showOpenDialog(s);
                     
                     if(file==null){return;}
                     
                     String path=file.getPath();
                     
                     initial_dir=path.substring(0,path.lastIndexOf(File.separator));
                     
                     MyFile my_file=new MyFile(path);
                     
                     calc_pgn();
                     
                     my_file.content=pgn;
                     
                     my_file.write_content();
                     
                    }
                
            });
            
            clip_box.getChildren().add(open_pgn_button);
            
            Button clip_to_fen_button=new Button();
            clip_to_fen_button.setText("Clip->Fen");
            clip_to_fen_button.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    String fen=clip.getString();
                    if(fen!=null)
                    {
                        b.set_from_fen(fen);
                        b.drawBoard();
                    }
                }
            });
            
            Button fen_to_clip_button=new Button();
            fen_to_clip_button.setText("Fen->Clip");
            fen_to_clip_button.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(b.report_fen());
                    clip.setContent(content);
                }
            });
            
            Button clip_to_pgn_button=new Button();
            clip_to_pgn_button.setText("Clip->PGN");
            clip_to_pgn_button.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    String pgn=clip.getString();
                    if(pgn!=null)
                    {
                        pgn_lines = pgn.split("\\r?\\n");
                        set_from_pgn_lines();
                    }
                }
            });
            
            Button pgn_to_clip_button=new Button();
            pgn_to_clip_button.setText("PGN->Clip");
            pgn_to_clip_button.setOnAction(new EventHandler<ActionEvent>() {
                @Override public void handle(ActionEvent e) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(calc_pgn());
                    clip.setContent(content);
                }
            });
            
            clip_box.getChildren().add(clip_to_fen_button);
            clip_box.getChildren().add(fen_to_clip_button);
            clip_box.getChildren().add(clip_to_pgn_button);
            clip_box.getChildren().add(pgn_to_clip_button);
            
            vertical_box.getChildren().add(clip_box);
            
            list.setMaxWidth(120);
            
            vertical_box.getChildren().add(list);
            
            save_pgn_box.getChildren().add(save_as_pgn_button);
            pgn_name_text.setMaxWidth(300);
            save_pgn_box.getChildren().add(pgn_name_text);
            save_pgn_box.getChildren().add(save_to_pgn_button);
            
            vertical_box.getChildren().add(save_pgn_box);
            
            pgn_text.setWrapText(true);
            
            pgn_text.setStyle("-fx-display-caret: false;");
            
            pgn_text.setOnMouseClicked(mouseHandler);
            
            vertical_box.getChildren().add(pgn_text);
            
            list.setOnMouseClicked(new EventHandler<Event>() {

                        @Override
                        public void handle(Event event) {
                            
                            int selected =  list.getSelectionModel().getSelectedIndex();

                            String pos=initial_position;
                            if(selected>0){pos=positions[selected-1];}
                            
                            game_ptr=selected;
                            
                            b.set_from_fen_inner(pos,false);
                            b.drawBoard();
                            
                            update_game();

                    }

                });
            
            
        }
    
}
