/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.infosystem.hatchet;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.tomahawk.libtomahawk.infosystem.deserializer.AlbumsDeserializer;
import org.tomahawk.libtomahawk.infosystem.deserializer.ArtistsDeserializer;
import org.tomahawk.libtomahawk.infosystem.deserializer.PlaylistsDeserializer;
import org.tomahawk.libtomahawk.infosystem.deserializer.TracksDeserializer;
import org.tomahawk.libtomahawk.infosystem.deserializer.UsersDeserializer;

import java.util.List;
import java.util.Map;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class HatchetSocialActionResponse {

    public List<HatchetSocialAction> socialActions;

    @JsonDeserialize(using = TracksDeserializer.class)
    public Map<String, HatchetTrackInfo> tracks;

    @JsonDeserialize(using = ArtistsDeserializer.class)
    public Map<String, HatchetArtistInfo> artists;

    @JsonDeserialize(using = AlbumsDeserializer.class)
    public Map<String, HatchetAlbumInfo> albums;

    @JsonDeserialize(using = PlaylistsDeserializer.class)
    public Map<String, HatchetPlaylistInfo> playlists;

    @JsonDeserialize(using = UsersDeserializer.class)
    public Map<String, HatchetUserInfo> users;

    public HatchetSocialActionResponse() {
    }
}
