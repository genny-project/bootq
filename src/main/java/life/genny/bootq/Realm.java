package life.genny.bootq;

public class Realm extends SheetReferralType<RealmUnit> {

    public Realm(String sheetURI) {
        super(sheetURI);
    }

    @Override
    public void init() {
        setDataUnits(getService().fetchRealmUnit(sheetURI));
    }
}