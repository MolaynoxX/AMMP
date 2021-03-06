package de.molaynoxx.amperfi.ui.controller;

import de.molaynoxx.amperfi.Amperfi;
import de.molaynoxx.amperfi.database.model.QLibraryFile;
import de.molaynoxx.amperfi.id3.ID3Helper;
import de.molaynoxx.amperfi.player.playlist.DatabasePlaylist;
import de.molaynoxx.amperfi.player.playlist.EntireLibraryPlaylist;
import de.molaynoxx.amperfi.player.playlist.Playlist;
import de.molaynoxx.amperfi.ui.controls.Sidebar;
import de.molaynoxx.amperfi.ui.controls.state.SidebarState;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class SidebarController extends AbstractController<Sidebar> {

    public enum LibraryViewMode {
        LIBRARY, TAG, PLAYLIST, SEARCH
    }

    private static PseudoClass activated = PseudoClass.getPseudoClass("activated");
    private static PseudoClass playing = PseudoClass.getPseudoClass("playing");

    public final EventHandler<? super MouseEvent> displayLibraryHandler = new DisplayLibraryHandler();
    public final ListChangeListener<? super TreeItem<Playlist>> displayArtistHandler = new DisplayTagHandler(ID3Helper.ID3Tag.ARTIST);
    public final ListChangeListener<? super TreeItem<Playlist>> displayAlbumHandler = new DisplayTagHandler(ID3Helper.ID3Tag.ALBUM);
    public final ListChangeListener<? super TreeItem<Playlist>> displayGenreHandler = new DisplayTagHandler(ID3Helper.ID3Tag.GENRE);
    public final EventHandler<? super MouseEvent> playLibraryHandler = new PlayLibraryHandler();
    public final EventHandler<? super MouseEvent> clickArtistHandler = new ClickTagHandler(ID3Helper.ID3Tag.ARTIST);
    public final EventHandler<? super MouseEvent> clickAlbumHandler = new ClickTagHandler(ID3Helper.ID3Tag.ALBUM);
    public final EventHandler<? super MouseEvent> clickGenreHandler = new ClickTagHandler(ID3Helper.ID3Tag.GENRE);

    public SidebarController(Sidebar control) {
        super(control);

        Amperfi.playbackController.currentlyPlayingProperty().addListener(new PlaylistChangedListener());
    }

    public void updateSidebar() {
        control.treeViewArtists.getRoot().getChildren().clear();
        for (String artist : Amperfi.db.getTags(ID3Helper.ID3Tag.ARTIST)) {
            if (artist == null) continue;
            control.treeViewArtists.getRoot().getChildren().add(new TreeItem<>(new DatabasePlaylist(artist, QLibraryFile.LibraryFile.artist.like(artist))));
        }

        control.treeViewAlbums.getRoot().getChildren().clear();
        for (String album : Amperfi.db.getTags(ID3Helper.ID3Tag.ALBUM)) {
            if (album == null) continue;
            control.treeViewAlbums.getRoot().getChildren().add(new TreeItem<>(new DatabasePlaylist(album, QLibraryFile.LibraryFile.album.like(album))));
        }

        control.treeViewGenres.getRoot().getChildren().clear();
        for (String genre : Amperfi.db.getTags(ID3Helper.ID3Tag.GENRE)) {
            if (genre == null) continue;
            control.treeViewGenres.getRoot().getChildren().add(new TreeItem<>(new DatabasePlaylist(genre, QLibraryFile.LibraryFile.genre.like(genre))));
        }
    }

    /**
     * Shows the library screen in the main area with the desired state (entire library/playlist/artist/genre/etc.)
     * Clears selection in other TreeViews so only one element is shown as active at a time
     *
     * @param state desired state
     */
    public void showMode(SidebarState state) {
        Amperfi.ui.showView(Amperfi.ui.libraryView);
        if (state.getLibraryViewMode() == LibraryViewMode.LIBRARY)
            control.lblLibrary.pseudoClassStateChanged(activated, true);
        else
            control.lblLibrary.pseudoClassStateChanged(activated, false);

        if (state.getTag() != ID3Helper.ID3Tag.ARTIST)
            control.treeViewArtists.getSelectionModel().clearSelection();
        if (state.getTag() != ID3Helper.ID3Tag.ALBUM)
            control.treeViewAlbums.getSelectionModel().clearSelection();
        if (state.getTag() != ID3Helper.ID3Tag.GENRE)
            control.treeViewGenres.getSelectionModel().clearSelection();

        Amperfi.ui.libraryView.controller.showPlaylist(state.getPlaylist());
    }

    /**
     * Gets called upon clicking the "Library" label
     * Brings the entire library up on the main section
     */
    private final class DisplayLibraryHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            showMode(new SidebarState(LibraryViewMode.LIBRARY, EntireLibraryPlaylist.INSTANCE));
        }

    }

    /**
     * Gets called upon changing the selected item inside one of the TreeViews in the sidebar (i.e. a band/album/genre)
     */
    private final class DisplayTagHandler implements ListChangeListener<TreeItem<Playlist>> {

        private final ID3Helper.ID3Tag tag;

        public DisplayTagHandler(ID3Helper.ID3Tag tag) {
            this.tag = tag;
        }

        @Override
        public void onChanged(Change<? extends TreeItem<Playlist>> c) {
            if (c.getList().size() == 0) return;
            Platform.runLater(() -> showMode(new SidebarState(LibraryViewMode.TAG, c.getList().get(0).getValue(), tag)));
        }

    }

    /**
     * Gets called upon double clicking the "Library" element in the sidebar and starts playback of the entire library
     */
    private final class PlayLibraryHandler implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                Amperfi.playbackController.playPlaylist(EntireLibraryPlaylist.INSTANCE);
            }
        }

    }

    /**
     * Gets called upon clicking an item inside one of the TreeViews in the sidebar (i.e. a band/album/genre)
     * Starts playback of the currently selected item in the sidebar if double clicked
     * Brings back correct screen for main area
     */
    private class ClickTagHandler implements EventHandler<MouseEvent> {

        private final ID3Helper.ID3Tag tag;

        public ClickTagHandler(ID3Helper.ID3Tag tag) {
            this.tag = tag;
        }

        @Override
        public void handle(MouseEvent event) {
            if (event.getButton() == MouseButton.PRIMARY) {
                Amperfi.ui.showView(Amperfi.ui.libraryView);
            }
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                switch (tag) {
                    case ARTIST:
                        Amperfi.playbackController.playPlaylist(control.treeViewArtists.getSelectionModel().getSelectedItem().getValue());
                        break;
                    case ALBUM:
                        Amperfi.playbackController.playPlaylist(control.treeViewAlbums.getSelectionModel().getSelectedItem().getValue());
                        break;
                    case GENRE:
                        Amperfi.playbackController.playPlaylist(control.treeViewGenres.getSelectionModel().getSelectedItem().getValue());
                        break;
                }
            }
        }

    }

    private class PlaylistChangedListener implements ChangeListener<Playlist> {

        @Override
        public void changed(ObservableValue<? extends Playlist> observable, Playlist oldValue, Playlist newValue) {
            control.lblLibrary.pseudoClassStateChanged(playing, newValue == EntireLibraryPlaylist.INSTANCE);
        }

    }

}