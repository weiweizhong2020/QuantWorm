/**
 * Filename: WormInfo.java This class define indivdal worm and its analysis
 * result
 */
package org.quantworm.wormgender;

public class WormInfo implements Comparable<WormInfo> {

    public int nHerma;
    public int nMale;
    public int nY;
    public int pX;
    public int pY;
    public int width;
    public int height;
    public double f;
    public double trueLen;
    public int wormLen;
    public double[] e;
    protected boolean suspicious;
    protected String viewingStatus;
    public int maskImageIDNumb;
    public static final String SUSPICIOUS = "unknown";
    public static final String MALE = "male";
    public static final String HERMAPHRODITE = "hermaphrodite";
    public static final String NOTHING = "nothing";
    public static final String MULTIPLE = "multiple[";
    public static final String DELETED = "deleted";

    /**
     * Default constructor
     */
    public WormInfo() {
        this.nHerma = 0;
        this.nMale = 0;
        this.pX = 0;
        this.pY = 0;
        this.width = 0;
        this.height = 0;
        this.trueLen = 0.000;
        this.wormLen = 0;
        this.f = 0.000;
        this.e = new double[10];
        this.e[0] = 0.000;
        this.e[1] = 0.000;
        this.suspicious = false;
        this.viewingStatus = null;
        this.maskImageIDNumb = 0;
    }

    /**
     * Constructor that takes boolean for suspicious status
     *
     * @param suspicious the suspicious value
     */
    public WormInfo(boolean suspicious) {
        this();
        this.suspicious = suspicious;
    }

    /**
     * Is this worm suspicious
     *
     * @return true if suspicious, otherwise: false
     */
    public boolean isSuspicious() {
        return suspicious;
    }

    /**
     * Sets this worm's suspicious flag
     *
     * @param flag either true (suspicious) or false (not suspicious)
     */
    public void setSuspicious(boolean flag) {
        this.suspicious = flag;
    }

    /**
     * Returns the viewing-status
     *
     * @return the viewing-status (null if it has not changed)
     */
    public String getViewingStatus() {
        return viewingStatus;
    }

    /**
     * Sets the viewing status (either male, hermaphrodite, multiple, deleted)
     *
     * @param status the new status
     */
    public void setViewingStatus(String status) {
        if (status == null) {
            viewingStatus = null;
            return;
        }

        if (status.startsWith(MULTIPLE) == true || status.equals(DELETED) == true
                || status.equals(MALE) == true || status.equals(HERMAPHRODITE) == true
                || status.equals(NOTHING) == true || status.equals(SUSPICIOUS) == true) {
            viewingStatus = status;
            if (status.equals(MULTIPLE + "0,0]") == true) {
                viewingStatus = NOTHING;
            }
        } else {
            System.out.println("ERROR, status value is not acceptable (" + status + ")");
            System.exit(1);
        }
    }

    /**
     * Get a human-readable label depending on number of males, hermaphrodites
     *
     * @return
     */
    public String getLabel() {
        // figure out the label of the original data (original-label)
        String original = getOriginalStatus();
        String originalLabel = null;
        if (original.startsWith(MULTIPLE) == true) {
            if (nMale == 1) {
                originalLabel = "<b>1</b> &#9794; " + MALE;
            }
            if (nMale > 1) {
                originalLabel = "<b>" + nMale + "</b> &#9794; " + MALE + "s";
            }
            // now add to the string the hermaphrodites
            if (nHerma == 1) {
                originalLabel = originalLabel == null ? "" : originalLabel + "<br />";
                originalLabel += "<b>1</b> " + HERMAPHRODITE;
            }
            if (nHerma > 1) {
                originalLabel = originalLabel == null ? "" : originalLabel + "<br />";
                originalLabel += "<b>" + nHerma + "</b> " + HERMAPHRODITE + "s";
            }
        } else {
            originalLabel = original;
            if (MALE.equals(originalLabel) == true) {
                originalLabel = "&#9794; " + originalLabel;
            }
            if (HERMAPHRODITE.equals(originalLabel) == true) {
                originalLabel = " " + originalLabel;
            }
        }

        if (original.equals(viewingStatus) == true || viewingStatus == null) {
            return "<html>" + originalLabel + "</html>";
        }

        String ret = null;
        if (viewingStatus.startsWith(MULTIPLE) == true) {
            Integer mm = getViewingMales();
            Integer hh = getViewingHermaphrodites();
            if (mm == 1) {
                ret = "<b>1</b> &#9794; " + MALE;
            }
            if (mm > 1) {
                ret = "<b>" + mm + "</b> &#9794; " + MALE + "s";
            }
            // now add to the string the hermaphrodites
            if (hh == 1) {
                ret = ret == null ? "" : ret + "<br />";
                ret += "<b>1</b> " + HERMAPHRODITE;
            }
            if (hh > 1) {
                ret = ret == null ? "" : ret + "<br />";
                ret += "<b>" + hh + "</b> " + HERMAPHRODITE + "s";
            }
        } else {
            ret = viewingStatus;
            if (MALE.equals(ret) == true) {
                ret = "&#9794; " + ret;
            }
            if (HERMAPHRODITE.equals(ret) == true) {
                ret = " " + ret;
            }
        }

        return "<html><font color=blue>" + ret + "<br/></font></html>";
    }

    /**
     * Get a human-readable label (no html) depending on number of males,
     * hermaphrodites
     *
     * @return
     */
    public String getLabelNoHtml() {
        // figure out the label of the original data (original-label)
        String original = getOriginalStatus();
        String originalLabel = null;
        if (original.startsWith(MULTIPLE) == true) {
            if (nMale == 1) {
                originalLabel = "1 " + MALE;
            }
            if (nMale > 1) {
                originalLabel = nMale + " " + MALE + "s";
            }
            // now add to the string the hermaphrodites
            if (nHerma == 1) {
                originalLabel = originalLabel == null ? "" : originalLabel + ", ";
                originalLabel += "1 " + HERMAPHRODITE;
            }
            if (nHerma > 1) {
                originalLabel = originalLabel == null ? "" : originalLabel + ", ";
                originalLabel += nHerma + " " + HERMAPHRODITE + "s";
            }
        } else {
            originalLabel = original;
        }

        if (original.equals(viewingStatus) == true || viewingStatus == null) {
            return originalLabel;
        }

        String ret = null;
        if (viewingStatus.startsWith(MULTIPLE) == true) {
            Integer mm = getViewingMales();
            Integer hh = getViewingHermaphrodites();
            if (mm == 1) {
                ret = "1 " + MALE;
            }
            if (mm > 1) {
                ret = mm + " " + MALE + "s";
            }
            // now add to the string the hermaphrodites
            if (hh == 1) {
                ret = ret == null ? "" : ret + ", ";
                ret += "1 " + HERMAPHRODITE;
            }
            if (hh > 1) {
                ret = ret == null ? "" : ret + ", ";
                ret += hh + " " + HERMAPHRODITE + "s";
            }
        } else {
            ret = viewingStatus;
        }

        return ret;
    }

    // get the number of males in MULTIPLE viewing-status
    // returns zero when viewing status is null or it is not 'multiple'
    public Integer getViewingMales() {
        if (viewingStatus == null) {
            return 0;
        }
        if (viewingStatus.startsWith(MULTIPLE) == false) {
            return 0;
        }
        return Integer.parseInt(viewingStatus.substring(MULTIPLE.length(), viewingStatus.indexOf(",")));
    }

    // get the number of hermaphrodites in MULTIPLE viewing-status
    // returns zero when viewing status is null or it is not 'multiple'
    public Integer getViewingHermaphrodites() {
        if (viewingStatus == null) {
            return 0;
        }
        if (viewingStatus.startsWith(MULTIPLE) == false) {
            return 0;
        }
        return Integer.parseInt(viewingStatus.substring(viewingStatus.indexOf(",") + 1, viewingStatus.length() - 1));
    }

    /**
     * Get the original status
     *
     * @return the original status (either of: male, hermaphrodite,
     * multiple[m,h], nothing, suspicious )
     */
    public String getOriginalStatus() {
        if (isSuspicious() == true) {
            return SUSPICIOUS;
        }

        if (nMale == 1 && nHerma == 0) {
            return MALE;
        }

        if (nMale == 0 && nHerma == 1) {
            return HERMAPHRODITE;
        }

        if (nMale == 0 && nHerma == 0) {
            return NOTHING;
        }

        return MULTIPLE + nMale + "," + nHerma + "]";
    }

    /**
     * Get number of males for the spinner
     *
     * @return
     */
    public Integer getMalesForSpinner() {
        if (viewingStatus != null) {
            if (HERMAPHRODITE.equals(viewingStatus) == true
                    || SUSPICIOUS.equals(viewingStatus) == true
                    || DELETED.equals(viewingStatus) == true
                    || NOTHING.equals(viewingStatus) == true) {
                return 0;
            }
            if (MALE.equals(viewingStatus) == true) {
                return 1;
            }
            return getViewingMales();
        }

        return nMale;
    }

    /**
     * Determines whether the worm object is deleted: when viewing status is not
     * null: returns true when it is either DELETED or NOTHING; when viewing
     * status is null: returns true when it is either DELETED or NOTHING;
     *
     * @return true when deleted (viewing status takes priority)
     */
    public boolean isDeletedInAnyWay() {
        if (viewingStatus != null) {
            if (DELETED.equals(viewingStatus) == true
                    || NOTHING.equals(viewingStatus) == true) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines whether the worm object is suspicious
     *
     * @return true when suspicious (viewing status takes priority)
     */
    public boolean isSuspiciousInAnyWay() {
        if (viewingStatus != null) {
            if (SUSPICIOUS.equals(viewingStatus) == true) {
                return true;
            }
            return false;
        }
        return isSuspicious();
    }

    /**
     * Determines whether the worm object is 'nothing'
     *
     * @return true when 'nothing' (viewing status takes priority)
     */
    public boolean isNothingInAnyWay() {
        if (viewingStatus != null) {
            if (NOTHING.equals(viewingStatus) == true) {
                return true;
            }
            return false;
        }
        if (nMale == 0 && nHerma == 0) {
            return true;
        }
        return false;
    }

    /**
     * Get number of hermaphrodites for the spinner
     *
     * @return
     */
    public Integer getHermaphroditesForSpinner() {
        if (viewingStatus != null) {
            if (MALE.equals(viewingStatus) == true
                    || SUSPICIOUS.equals(viewingStatus) == true
                    || DELETED.equals(viewingStatus) == true
                    || NOTHING.equals(viewingStatus) == true) {
                return 0;
            }
            return getViewingHermaphrodites();
        }

        return nHerma;
    }

    /**
     * Get a label for original-status
     *
     * @return label for original status
     */
    public String getOriginalStatusLabel() {
        String ret = getOriginalStatus();
        if (ret.startsWith(MULTIPLE) == true) {
            ret = "";
            if (nMale == 1) {
                ret = "1 male";
            }
            if (nMale > 1) {
                ret = nMale + " males";
            }
            if (nHerma > 0) {
                ret = "".equals(ret) ? ret : ret + ", ";
                if (nHerma == 1) {
                    ret += "1 hermaphrodite";
                } else {
                    ret += nHerma + " hermaphrodites";
                }
            }
        }
        return ret;
    }

    @Override
    public int compareTo(WormInfo obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        // both statuses are same
        if (getOriginalStatus().equals(obj.getOriginalStatus()) == true) {
            if (hashCode() < obj.hashCode()) {
                return -1;
            }
            if (hashCode() > obj.hashCode()) {
                return 1;
            }
            return 0;
        }

        int ours = 0;
        int other = 0;

        if (SUSPICIOUS.equals(getOriginalStatus()) == true) {
            ours -= 40000;
        } else {
            // case: 1 MALE section
            if (nMale == 1 && nHerma == 0) {
                ours += 400000000;
            } else if (nMale > 0) {
                ours += nMale * 30000000;
            } else {
                ours += nHerma * 20000;
            }
            ours += pX + pY + width + height;
        }

        if (SUSPICIOUS.equals(obj.getOriginalStatus()) == true) {
            other -= 40004;
        } else {
            if (obj.nMale == 1 && obj.nHerma == 0) {
                other += 400000000;
            } else if (obj.nMale > 0) {
                other += obj.nMale * 30000000;
            } else {
                other += obj.nHerma * 20000;
            }
            other += obj.pX + obj.pY + obj.width + obj.height;
        }

        if (ours > other) {
            return -1;
        }
        if (ours < other) {
            return 1;
        }

        if (hashCode() < obj.hashCode()) {
            return -1;
        }
        if (hashCode() > obj.hashCode()) {
            return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof WormInfo) == false) {
            return false;
        }
        WormInfo other = (WormInfo) obj;
        return compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        return (nHerma + "," + nMale + "," + pX + ","
                + pY + "," + width + "," + height + ","
                + f + "," + trueLen + "," + wormLen + ","
                + suspicious + "," + e[ 0] + "," + e[ 1]).hashCode();
    }

    @Override
    public String toString() {
        return "[" + pX + "," + pY + "] males:" + nMale
                + " , herma:" + nHerma + " trueLenght: "
                + trueLen + " , unknown:" + suspicious
                + " , originalStatus:" + getOriginalStatus();
    }
}
