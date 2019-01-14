package org.hotteam67.firebaseviewer.tableview;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import com.evrencoskun.tableview.TableView;
import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.AdapterDataSetChangedListener;
import com.evrencoskun.tableview.adapter.recyclerview.CellRecyclerViewAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.RowHeaderRecyclerViewAdapter;
import com.evrencoskun.tableview.filter.Filter;
import com.evrencoskun.tableview.filter.FilterItem;
import com.evrencoskun.tableview.filter.FilterType;
import com.evrencoskun.tableview.filter.IFilterableModel;
import com.evrencoskun.tableview.sort.SortState;

import java.util.ArrayList;
import java.util.List;

/**
 * A TableView, but rewritten to support the MultiFilter, so filters are now inclusive and not exclusive.
 * This means a filter containing 67 and 33 is an OR which allows either to pass. Most of this code
 * is the SAME as the generic tableview.
 * Also has a sibling functionality where it will update the filter for another given table,
 * which is used for keeping Averages and Maximums in sync
 */
public class MultiFilterTableView extends TableView {

    private CellRecyclerViewAdapter mCellRecyclerViewAdapter;
    private RowHeaderRecyclerViewAdapter mRowHeaderRecyclerViewAdapter;
    private List<List<IFilterableModel>> originalCellDataStore;
    private List originalRowDataStore;

    /**
     * The sibling table view to sync filters with
     */
    private MultiFilterTableView sibling;

    public MultiFilterTableView(@NonNull Context context) {
        super(context);
    }

    public MultiFilterTableView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiFilterTableView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int
            defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Set the sibling table view to sync the filter with. One of only two changed functions
     * @param sibling the sibling table view to sync filters to
     */
    public void SetSiblingTableView(MultiFilterTableView sibling)
    {
        this.sibling = sibling;
    }

    @Override
    public void setAdapter(AbstractTableAdapter a)
    {
        super.setAdapter(a);
        FilterInit();
    }

    private void FilterInit() {
        getAdapter().addAdapterDataSetChangedListener(adapterDataSetChangedListener);
        this.mCellRecyclerViewAdapter = (CellRecyclerViewAdapter)
                getCellRecyclerView().getAdapter();

        this.mRowHeaderRecyclerViewAdapter = (RowHeaderRecyclerViewAdapter)
                getRowHeaderRecyclerView().getAdapter();
    }

    @Override
    public void sortColumn(int column, SortState sort)
    {
        sortColumn(column, sort, false);
    }

    private void sortColumn(int column, SortState sort, boolean skipSibling)
    {
        super.sortColumn(column, sort);
        if (!skipSibling && sibling != null)
            sibling.sortColumn(column, sort, true);
        this.scrollTo(0, 0);
    }

    @Override
    public void filter(Filter filter)
    {
        filter(filter, false);
    }

    @SuppressWarnings("unchecked")
    public void filter(Filter filter, boolean doContains) {
        filter(filter, doContains, false);
    }

    /**
     * The modified filter function
     * @param filter the filter class to use
     * @param doContains whether to use contains() or equals(), contains() if true
     * @param skipSibling whether to skip the sibling table provided, set to true if this filter update
     *                    was already triggered by a sibling, to prevent Stack Overflow
     */
    private void filter(Filter filter, boolean doContains, boolean skipSibling)
    {
        if (originalCellDataStore == null || originalRowDataStore == null) {
            return;
        }

        List<List<IFilterableModel>> originalCellData = new ArrayList<>(originalCellDataStore);
        List originalRowData = new ArrayList<>(originalRowDataStore);
        List<List<IFilterableModel>> filteredCellList = new ArrayList<>();
        List filteredRowList = new ArrayList<>();

        if (filter.getFilterItems().isEmpty()) {
            filteredCellList = new ArrayList<>(originalCellDataStore);
            filteredRowList = new ArrayList<>(originalRowDataStore);
        } else {
            for (int x = 0; x < filter.getFilterItems().size(); ++x) {
                final FilterItem filterItem = filter.getFilterItems().get(x);
                if (filterItem.getFilterType().equals(FilterType.ALL)) {
                    for (List<IFilterableModel> itemsList : originalCellData) {
                        for (IFilterableModel item : itemsList) {
                            if (item
                                    .getFilterableKeyword()
                                    .toLowerCase()
                                    .equals(filterItem
                                            .getFilter()
                                            .toLowerCase())) {
                                filteredCellList.add(itemsList);
                                filteredRowList.add(originalRowData.get(filteredCellList.indexOf(itemsList)));
                                break;
                            }
                        }
                    }
                } else {
                    for (List<IFilterableModel> itemsList : originalCellData) {
                        String s1 = itemsList.get(filterItem.getColumn()).getFilterableKeyword().toLowerCase();
                        String s2 = filterItem.getFilter().toLowerCase();
                        boolean pass = (doContains) ? s1.contains(s2) : s1.equals(s2);
                        if (pass) {
                            filteredCellList.add(itemsList);
                            filteredRowList.add(originalRowData.get(originalCellData.indexOf(itemsList)));
                        }
                    }
                }

                /*
                // If this is the last filter to be processed, the filtered lists will not be cleared.
                if (++x < filter.getFilterItems().size()) {
                    originalCellData = new ArrayList<>(filteredCellList);
                    originalRowData = new ArrayList<>(filteredRowList);
                    filteredCellList.clear();
                    filteredRowList.clear();
                }
                */
            }
        }

        // Sets the filtered data to the TableView.
        mRowHeaderRecyclerViewAdapter.setItems(filteredRowList, true);
        mCellRecyclerViewAdapter.setItems(filteredCellList, true);
        if (!skipSibling && sibling != null)
        {
            sibling.filter(filter, doContains, true);
        }
    }

    @SuppressWarnings("unchecked")
    private final AdapterDataSetChangedListener adapterDataSetChangedListener =
            new AdapterDataSetChangedListener() {
                @Override
                public void onRowHeaderItemsChanged(List rowHeaderItems) {
                    if (rowHeaderItems != null) {
                        originalRowDataStore = new ArrayList<>(rowHeaderItems);
                    }
                }

                @Override
                public void onCellItemsChanged(List cellItems) {
                    if (cellItems != null) {
                        originalCellDataStore = new ArrayList<>(cellItems);
                    }
                }
            };
}

