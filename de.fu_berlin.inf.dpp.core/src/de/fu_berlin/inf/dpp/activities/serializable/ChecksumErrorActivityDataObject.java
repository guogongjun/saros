/*
 * DPP - Serious Distributed Pair Programming
 * (c) Freie Universität Berlin - Fachbereich Mathematik und Informatik - 2006
 * (c) Riad Djemili - 2006
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package de.fu_berlin.inf.dpp.activities.serializable;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.activities.business.ChecksumErrorActivity;
import de.fu_berlin.inf.dpp.activities.business.IActivity;
import de.fu_berlin.inf.dpp.session.User;

/**
 * A Checksum Error is a notification send to the host and peers by a user, who
 * wants inconsistencies to be recovered.
 */
@XStreamAlias("checksumErrorActivity")
public class ChecksumErrorActivityDataObject extends AbstractActivityDataObject {

    @XStreamAsAttribute
    protected final User target;

    @XStreamAsAttribute
    protected String recoveryID;

    @XStreamImplicit
    protected List<SPath> paths;

    public ChecksumErrorActivityDataObject(User source, User target,
        List<SPath> paths, String recoveryID) {

        super(source);

        this.target = target;
        this.paths = paths;
        this.recoveryID = recoveryID;
    }

    @Override
    public IActivity getActivity() {

        return new ChecksumErrorActivity(getSource(), target, paths, recoveryID);
    }

    @Override
    public String toString() {
        return "ChecksumErrorActivityDO(src: " + getSource() + ", paths: "
            + paths + ", recoveryID: " + recoveryID + ")";
    }
}
