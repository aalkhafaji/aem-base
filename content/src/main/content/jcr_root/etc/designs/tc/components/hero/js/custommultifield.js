/**
 * Copyright (C) 2014 Virtusa Corporation.
 * This file is proprietary and part of Virtusa LaunchPad.
 * LaunchPad code can not be copied and/or distributed without the express permission of Virtusa Corporation
 */

/**
 * @class CQ.form.MultiCompositeField
 * @extends CQ.form.CompositeField
 * The MultiCompositeField is an editable list of a set of form fields for editing
 * a list of nodes with their properties. Unlike the {@link CQ.form.MultiField},
 * which works on an array of values (multi-value property in JCR), this widget works
 * on a list of named objects, each containing the same set of properties (nodes with
 * properties in JCR).
 *
 * <p>The naming scheme for the nodes will use a baseName + an automatically incremented
 * number, eg. "node_1" where the baseName is "node_". For better readability, the number
 * can be replaced by using the value of one of the fields (using {@link #nameField},
 * optionally regexp-based using {@link #nameFieldRegex}), eg. "node_valueA", "node_valueB".
 * Note that if ordering is desired (via {@link #orderable}), it will be managed
 * independently from the numbering, only the used node type must support it.
 * Additionally, a prefix can be given for the final field names (just for the submit
 * field names, eg. to support the often required "./" prefix for the Sling POST).
 *
 * @constructor
 * Creates a new MultiCompositeField.
 * @param {Object} config The config object
 */
CQ.form.MultiCompositeField = CQ.Ext.extend(CQ.form.CompositeField, {

    /**
     * @cfg {String} prefix
     * A general prefix added to every field name (eg. for "./") for submit.
     * Optional.
     */
     
    /**
     * @cfg {String} name
     * The container node for the list of managed nodes. If this is set,
     * the individual items to load will be taken from the child nodes
     * of that container node (eg. "container/node_1", "container/node_2"),
     * otherwise from the "current" node, ie. on the same level as the
     * other fields next to this one (eg. "node_1", "node_2"). In the latter
     * case it is probably desired to set {@link #baseName} and {@link #matchBaseName}
     * to filter out the correct nodes (eg. "baseName_1", "baseName_2").
     * Optional.
     */
     
    /**
     * @cfg {String} baseName
     * A baseName for the node names of the individual objects, eg. "file_". Will
     * be used to create the names for new nodes. If {@link #matchBaseName} is true,
     * it will also be used to filter out the nodes to load. Defaults to "item_".
     */
     
    /**
     * @cfg {Boolean} matchBaseName
     * Whether nodes must match the {@link #baseName} when loading the items.
     * If not, all objects/nodes found are loaded. Defaults to true.
     */
     
// TODO later BEGIN
    /**
     * @cfg {Boolean} orderable
     * NOT SUPPORTED YET
     * If the list of fields should be orderable and Up/Down buttons
     * are rendered (defaults to true).
     */
     
    /**
     * @cfg {String} nameField
     * NOT SUPPORTED YET
     * Name of a sub field from which to use the value as part of the node name,
     * instead of an incrementing number. The value must be string-like. To use
     * only a part of it, a regexp can be defined via {@link #nameFieldRegex}. Optional.
     */
     
    /**
     * @cfg {RegExp} nameFieldRegex
     * NOT SUPPORTED YET
     * A Javascript regular expression to further filter out the value from the field
     * given by {@link #nameField}. If the regexp defines a group, the match result of
     * the group is used, otherwise the full matching part. Optional.
     */
// TODO later END
     
    /**
     * @cfg {Array} fieldConfigs
     * An array of configuration options for the fields. Required.
     * <p>Example:
     * <pre><code>
[{
     xtype: "textfield",
     name: "key",
     fieldLabel: "Key"
},{
     xtype: "textfield",
     name: "value",
     fieldLabel: "Value"
}]      </code></pre>
     */
    
    /**
     * @cfg {Object} itemPanelConfig
     * A config for the panel that holds the fields defined in {@link #fieldConfigs}.
     * Can be used to define the layout further. The "items" object will be overwritten.
     * Defaults to a simple panel with a from layout.
     */

    // private
    path: "",

    bodyPadding: 0,

    // the width of the field
    // private
    fieldWidth: 0,

    constructor: function(config) {
        var list = this;
         this.minLimit = config.minLimit;
         if(config.fieldLabel){
         this.fieldLabel= config.fieldLabel;
         } else{
         
          this.fieldLabel = "List";
         
         }
        var items = new Array();

        if(!config.readOnly) {
            items.push({
                xtype: "button",
                cls: "cq-multifield-btn",
                text: "+",
                handler: function() {
                //alert(config.maxLimit);
               // alert(list.items.getCount());
               if(config.maxLimit){
                if(list.items.getCount()-1<config.maxLimit){
                    list.addItem();
                }else{
                
                alert("Max Limit"+" "+config.maxLimit+" "+"Reached");
                
                }
                }else{
                   list.addItem();
                
                }
                }
            });
        }

        if (config.name) {
            // TODO: change to delete list (triggered after first reordering)
            this.hiddenDeleteField = new CQ.Ext.form.Hidden({
                "name":config.name + CQ.Sling.DELETE_SUFFIX
            });
            items.push(this.hiddenDeleteField);
        }

        config = CQ.Util.applyDefaults(config, {
            fieldConfigs: [],
            itemPanelConfig: {
                xtype: "panel",
                layout: "form",
                border: false
            },
            orderable: true,
            baseName: "item_",
            matchBaseName: true,
            border: false,
            items:[{
                xtype: "panel",
                border: false,
                bodyStyle: "padding:0px",
                items: items
            }]
        });
        CQ.form.MultiCompositeField.superclass.constructor.call(this,config);

        // typical example: prefix="./", name="items" => "./items/"
        this.fieldNamePrefix = config.prefix || "";
        if (config.name) {
            this.fieldNamePrefix += config.name + "/";
        }
        
        // TODO: properly send change event
        
    },

    initComponent: function() {
        CQ.form.MultiCompositeField.superclass.initComponent.call(this);
    },

    // private
    calculateFieldWidth: function(item) {
        try {
            //this.fieldWidth = this.getSize().width - 2*this.bodyPadding; // total row width
            for (var i = 1; i < item.items.length; i++) {
                // subtract each button
                var w = item.items.get(i).getSize().width;
                if (w == 0) {
                    // button has no size, e.g. because MV is hidden >> reset fieldWidth to avoid setWidth
                    this.fieldWidth = 0;
                    return;
                }

                this.fieldWidth -= item.items.get(i).getSize().width;
            }
        }
        catch (e) {
            // initial resize fails if the MF is on the visible first tab
            // >> reset to 0 to avoid setWidth
            this.fieldWidth = 0;
        }
    },
    
    /**
     * Creates the name for a new field. Must take the baseName and append
     * a unique number;
     */
    createName: function() {
        for (var i = 1;; i++) {
            var name = this.baseName + i;
            
            // check if this name has been used
            var item = this.items.find(function(item) {
                return item.name == name;
            });
            if (!item) {
                return name;
            }
        }
        return "";
    },

    /**
     * Adds a new field with the specified value to the list.
     * @param {String} name name of the object to add
     * @param {Object} o The object to add
     */
    addItem: function(name, o) {
        if (!name) {
            // new item to add
            name = this.createName();
        }
        
        // what to do with values that couldn't be found? we delete the nodes normally...
        var item = this.insert(this.items.getCount() - 1, {
            xtype: "multicompositefielditem",
            name: name,
            prefix: this.fieldNamePrefix,
            orderable: this.orderable,
            readOnly: this.readOnly,
            fieldConfigs: this.fieldConfigs,
            panelConfig: this.itemPanelConfig
        });
        // TODO: add all fields - or maybe not, seems they get automatically added
        //this.findParentByType("form").getForm().add(item.field);
        this.doLayout();

        item.processPath(this.path);
        if (o) {
            item.setValue(o);
        }

        try {
            // TODO: field width
            //console.log("setPanelWidth", this.fieldWidth);
            //item.setPanelWidth(this.fieldWidth);
        }
        catch (e) {
            CQ.Log.debug("CQ.form.MultiCompositeField#addItem: " + e.message);
        }
    },
    
    processPath: function(path) {
        this.path = path;
    },

    // overriding CQ.form.CompositeField#getValue
    getValue: function() {
        var value = new Array();
        this.items.each(function(item, index/*, length*/) {
            if (item instanceof CQ.form.MultiCompositeField.Item) {
                value[index] = item.getValue();
                index++;
            }
        }, this);
        return value;
    },

    // private, loads a single object
    processItem: function(name, o) {
        if (typeof o !== "object") {
            return;
        }
        
        if (this.baseName && this.matchBaseName !== false) {
            // check if o.name starts with the baseName
            if (name.indexOf(this.baseName) !== 0) {
                return;
            }
        }
        //console.log("addItem", name);
        this.addItem(name, o);
    },
    
    // overriding CQ.form.CompositeField#processRecord
    processRecord: function(record, path) {
        
        if (this.fireEvent('beforeloadcontent', this, record, path) !== false) {
            
            // remove all existing fields
            this.items.each(function(item/*, index, length*/) {
                if (item instanceof CQ.form.MultiCompositeField.Item) {
                    this.remove(item, true);
                    // TODO: remove all fields from form
                    //this.findParentByType("form").getForm().remove(item);
                }
            }, this);
            
            if (this.name) {
                var c = record.get(this.name);
                for (var n in c) {
                    var v = record.get(this.getName());
                    this.processItem(n, c[n]);
                }
            } else {
                record.fields.each(function(field) {
                    this.processItem(field.name, record.get(field.name));
                }, this);
            }
            
            this.fireEvent('loadcontent', this, record, path);
             var dialog = this.findParentByType("dialog");
                if (dialog) {   
                     dialog.on("beforesubmit", function() {
                        return this.onBeforeSubmit();
                    }, this);
                    }
                }
        
        
        
    },
    
    onBeforeSubmit: function() { 
      if(this.minLimit){
      if((this.items.getCount()-1)<this.minLimit){
        
      alert("Please add atleast"+" "+this.minLimit+" "+ "records in"+" "+this.fieldLabel);
      return false;
      } else{
      return true;
      
      }
    } else{
      return true;
        }
    },

    // overriding CQ.form.CompositeField#setValue
  setValue: function(value) {
 
    }

});

CQ.Ext.reg("multicompositefield", CQ.form.MultiCompositeField);

/**
 * @private
 * @class CQ.form.MultiCompositeField.Item
 * @extends CQ.Ext.Panel
 * The MultiCompositeField.Item is an item in the {@link CQ.form.MultiCompositeField}.
 * This class is not intended for direct use.
 * @constructor
 * Creates a new MultiCompositeField.Item.
 * @param {Object} config The config object
 */
CQ.form.MultiCompositeField.Item = CQ.Ext.extend(CQ.Ext.Panel, {

    /**
     * @cfg {String} name
     * @member CQ.form.MultiCompositeField.Item
     * Name of this item.
     */
    /**
     * @cfg {String} prefix
     * @member CQ.form.MultiCompositeField.Item
     * Prefix to add to all field names.
     */
    /**
     * @cfg {Boolean} readOnly
     * @member CQ.form.MultiCompositeField.Item
     * If the fields should be read only.
     */
    /**
     * @cfg {Boolean} orderable
     * @member CQ.form.MultiCompositeField.Item
     * If order up/down buttons should be added.
     */
    /**
     * @cfg {Array} fieldConfigs
     * @member CQ.form.MultiCompositeField.Item
     * Array of field configurations.
     */
    /**
     * @cfg {Object} panelConfig
     * @member CQ.form.MultiCompositeField.Item
     * Config for panel holding fields.
     */
     
    constructor: function(config) {
        var item = this;
        
        var fields = CQ.Util.copyObject(config.fieldConfigs);
        for (var i = 0; i < fields.length; i++) {
            var f = fields[i];
            f.rawFieldName = f.name;
            f.name = config.prefix + config.name + "/" + f.rawFieldName;
            f.readOnly = config.readOnly;
        }

        config.panelConfig = CQ.Util.copyObject(config.panelConfig);
        config.panelConfig.items = fields;
        config.panelConfig.cellCls = "cq-multifield-itemct";
        
        var items = new Array();
        items.push(config.panelConfig);
        if(!config.readOnly) {
            
            // TODO: enable ordering again when functionality is implemented (see reorder())
          
            if (config.orderable) {
                items.push({
                    xtype: "panel",
                    border: false,
                    items: {
                        xtype: "button",
                        text: CQ.I18n.getMessage("Up", null, "Ordering upwards in MultiField"),
                        handler: function(){
                            var parent = item.ownerCt;
                            var index = parent.items.indexOf(item);
                            
                            if (index > 0) {
                                item.reorder(parent.items.itemAt(index - 1));
                            }
                        }
                    }
                });
                items.push({
                    xtype: "panel",
                    border: false,
                    items: {
                        xtype: "button",
                        text: CQ.I18n.getMessage("Down", null, "Ordering downwards in MultiField"),
                        handler: function(){
                            var parent = item.ownerCt;
                            var index = parent.items.indexOf(item);
                            
                            // note: there is one last item for the add button, must be ignored
                            if (index < parent.items.getCount() - 2) {
                                parent.items.itemAt(index + 1).reorder(item);
                            }
                        }
                    }
                });
            }

            items.push({
                xtype: "panel",
                border: false,
                items: {
                    xtype: "button",
                    cls: "cq-multifield-btn",
                    text: "-",
                    handler: function() {
                        // TODO: remove from owner form !???
                        item.ownerCt.remove(item);
                    }
                }
            });
        }

        config = CQ.Util.applyDefaults(config, {
            layout: "table",
            anchor: "100%",
            bodyCssClass: "cq-multifield-item",
            border: false,
            layoutConfig: {
                columns: 4
            },
            defaults: {
                bodyStyle: "padding:0px"
            },
            items: items
        });
        CQ.form.MultiCompositeField.Item.superclass.constructor.call(this, config);
        
        this.fields = new CQ.Ext.util.MixedCollection(false, function(field) {
            return field.rawFieldName;
        });
        this.getFieldPanel().items.each(function(item) {
            if (item.rawFieldName) {
                this.fields.add(item.rawFieldName, item);
            }
        }, this);

        if (config.value) {
            this.setValue(config.value);
        }
    },


    getFieldPanel: function() {
        return this.items.get(0);
    },

    setPanelWidth: function(w) {
        this.getFieldPanel().setWidth(w);
    },

    /**
     * Reorders the item above the specified item.
     * @param item {CQ.form.MultiCompositeField.Item} The item to reorder above
     * @member CQ.form.MultiCompositeField.Item
     */
    reorder: function(item) {
        var c = this.ownerCt;
        // move this item before the other one in the parent
        c.insert(c.items.indexOf(item), this);
        // must manually move dom element as well
        this.getEl().insertBefore(item.getEl());
        
        // TODO: notify parent of re-ordering to add @Delete hidden fields (if name == null)
        
        // TODO: for each field in this item, reorder it before the fields of the other item inside form.items
        //console.log(this.findParentByType("form").getForm().items);
        
        c.doLayout();
    },
    
    processPath: function(path) {
        this.fields.each(function(f) {
            if (f.processPath) {
                f.processPath(path);
            }
        });
    },
    
    /**
     * Returns the data value.
     * @return {Object} value The field value
     * @member CQ.form.MultiCompositeField.Item
     */
    getValue: function() {
        var o = {};
        this.fields.each(function(f) {
            o[f.rawFieldName] = f.getValue();
        });
        return o;
    },

    /**
     * Sets a data value into the field and validates it.
     * @param {Object} value The value object to set
     * @member CQ.form.MultiCompositeField.Item
     */
    setValue: function(value) {
        this.fields.each(function(f) {
            if (value[f.rawFieldName]) {
                f.setValue(value[f.rawFieldName]);
            }
        });
    }
});

CQ.Ext.reg("multicompositefielditem", CQ.form.MultiCompositeField.Item); 
