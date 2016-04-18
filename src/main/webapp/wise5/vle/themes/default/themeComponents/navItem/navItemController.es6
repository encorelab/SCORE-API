"use strict";

class NavItemController {
    constructor($rootScope,
                $scope,
                $element,
                ProjectService,
                StudentDataService) {

        this.$rootScope = $rootScope;
        this.$scope = $scope;
        this.$element = $element;
        this.ProjectService = ProjectService;
        this.StudentDataService = StudentDataService;

        this.expanded = false;

        this.item = this.ProjectService.idToNode[this.nodeId];
        this.isGroup = this.ProjectService.isGroupNode(this.nodeId);
        this.nodeStatuses = this.StudentDataService.nodeStatuses;
        this.nodeStatus = this.nodeStatuses[this.nodeId];

        this.nodeTitle = this.showPosition ? (this.ProjectService.idToPosition[this.nodeId] + ': ' + this.item.title) : this.item.title;
        this.currentNode = this.StudentDataService.currentNode;
        this.isCurrentNode = (this.currentNode.id === this.nodeId);
        this.setNewNode = false;
        
        // whether this node is a planning node
        this.isPlanning = this.ProjectService.isPlanning(this.nodeId);
        this.availablePlanningNodeIds = null;
        this.parentGroupId = null;
        
        var parentGroup = this.ProjectService.getParentGroup(this.nodeId);
        
        if (parentGroup != null) {
            this.parentGroupId = parentGroup.id;
            this.isParentGroupPlanning = this.ProjectService.isPlanning(this.parentGroupId);
        }
        
        if (this.isPlanning) {
            /*
             * planning is enabled so we will get the available planning
             * nodes that can be used in this group
             */
            this.availablePlanningNodeIds = this.ProjectService.getAvailablePlanningNodeIds(this.nodeId);
        }
        
        if (this.isParentGroupPlanning) {
            /*
             * planning is enabled so we will get the available planning
             * nodes that can be used in this group
             */
            this.availablePlanningNodeIds = this.ProjectService.getAvailablePlanningNodeIds(this.parentGroupId);
            
            /*
             * update the nodes in the select drop down used to move planning
             * nodes around
             */
            this.updateSiblingNodeIds();
            
            this.$scope.$watch(
                function () {
                    // watch the position of this node
                    return this.ProjectService.idToPosition[this.nodeId];
                }.bind(this),
                function(value) {
                    // the position has changed for this node so we will update it in the UI
                    this.nodeTitle = this.showPosition ? (this.ProjectService.idToPosition[this.nodeId] + ': ' + this.item.title) : this.item.title;
                    
                    /*
                     * update the nodes in the select drop down used to move planning
                     * nodes around
                     */
                    this.updateSiblingNodeIds();
                }.bind(this)
            );
        }

        this.$scope.$watch(
            function () { return this.StudentDataService.currentNode; }.bind(this),
            function (newNode) {
                this.currentNode = newNode;
                if (this.StudentDataService.previousStep) {
                    this.$scope.$parent.isPrevStep = (this.nodeId === this.StudentDataService.previousStep.id);
                }
                this.isCurrentNode = (this.currentNode.id === this.nodeId);
                if (this.isCurrentNode || this.ProjectService.isApplicationNode(newNode.id) || newNode.id === this.ProjectService.rootNode.id) {
                    this.setExpanded();
                }
            }.bind(this)
        );

        this.$scope.$watch(
            function () { return this.expanded; }.bind(this),
            function (value) {
                this.$scope.$parent.itemExpanded = value;
                if (value) {
                    this.zoomToElement();
                }
            }.bind(this)
        );
        
        this.$rootScope.$on('planningNodeChanged', () => {
            /*
             * update the nodes in the select drop down used to move planning
             * nodes around
             */
            this.updateSiblingNodeIds();
        });

        this.setExpanded();
    }
    
    updateSiblingNodeIds() {
        var childNodeIds = this.ProjectService.getChildNodeIdsById(this.parentGroupId);
        
        this.siblingNodeIds = [];
        this.siblingNodeIds.push(this.parentGroupId);
        this.siblingNodeIds = this.siblingNodeIds.concat(childNodeIds);
    }

    getTemplateUrl(){
        return this.ProjectService.getThemePath() + '/themeComponents/navItem/navItem.html';
    };

    setExpanded() {
        this.$scope.expanded = (this.isCurrentNode || (this.$scope.isGroup && this.ProjectService.isNodeDescendentOfGroup(this.$scope.currentNode, this.$scope.item)));
        if (this.$scope.expanded && this.isCurrentNode) {
            this.expanded = true;
            this.zoomToElement();
        }
    };

    zoomToElement() {
        setTimeout(()=> {
            // smooth scroll to expanded group's page location
            let location = this.isGroup ? this.$element[0].offsetTop - 32 : 0;
            let delay = this.isGroup ? 350 : 0;
            $('#content').animate({
                scrollTop: location
            }, delay, 'linear', ()=> {
                if (this.setNewNode) {
                    this.setNewNode = false;
                    this.StudentDataService.endCurrentNodeAndSetCurrentNodeByNodeId(this.nodeId);
                }
            });
        }, 250);
    };

    itemClicked() {
        if (this.isGroup) {
            if (!this.expanded) {
                this.setNewNode = true;
            }
            this.expanded = !this.expanded;
        } else {
            this.StudentDataService.endCurrentNodeAndSetCurrentNodeByNodeId(this.nodeId);
        }
    };
    
    /**
     * Create a planning node instance and add it to the project
     * @param groupId the group the new planning node instance will be added to
     * @param templateNodeId the node id of the planning node template
     */
    addPlanningNodeInstanceInside(nodeIdToInsertInside, templateNodeId) {
        // create the planning node instance
        var planningNodeInstance = this.ProjectService.createPlanningNodeInstance(nodeIdToInsertInside, templateNodeId);
        
        // add the planning node instance inside
        this.ProjectService.addPlanningNodeInstanceInside(nodeIdToInsertInside, planningNodeInstance);
        
        /*
         * update the node statuses so that a node status is created for
         * the new planning node instance
         */
        this.StudentDataService.updateNodeStatuses();
        
        // perform any necessary updating
        this.planningNodeChanged();
    }
    
    /**
     * Create a planning node instance and add it to the project
     * @param groupId the group the new planning node instance will be added to
     * @param nodeId the node id of the planning node template
     */
    addPlanningNodeInstanceAfter(nodeIdToInsertAfter, templateNodeId) {
        
        var parentGroup = this.ProjectService.getParentGroup(nodeIdToInsertAfter);
        
        if (parentGroup != null) {
            var parentGroupId = parentGroup.id;
            
            // create the planning node instance
            var planningNodeInstance = this.ProjectService.createPlanningNodeInstance(parentGroupId, templateNodeId);
            
            // insert planning node instance after
            this.ProjectService.addPlanningNodeInstanceAfter(nodeIdToInsertAfter, planningNodeInstance);
            
            /*
             * update the node statuses so that a node status is created for
             * the new planning node instance
             */
            this.StudentDataService.updateNodeStatuses();
            
            // perform any necessary updating
            this.planningNodeChanged();
        }
    }
    
    /**
     * Remove the planning node instance
     * @param planningNodeInstanceNodeId the planning node instance to remove
     */
    removePlanningNodeInstance(planningNodeInstanceNodeId) {
        // delete the node from the project
        this.ProjectService.deleteNode(planningNodeInstanceNodeId);
        
        // perform any necessary updating
        this.planningNodeChanged();
    }
    
    /**
     * Get the node title
     * @param nodeId get the title for this node
     * @returns the title for the node
     */
    getNodeTitle(nodeId) {
        var node = this.ProjectService.idToNode[nodeId];
        var title = null;
        
        if (node != null) {
            title = node.title;
        }
        
        // get the position
        var position = this.ProjectService.idToPosition[nodeId];
        
        if (position == null) {
            return title;
        } else {
            return position + ': ' + title;
        }
    }
    
    /**
     * Move the planning node. If the other node is a group node, we will
     * insert this node as the first node in the group. If the other node is
     * a step node, we will insert this node after the other node.
     * @param otherNodeId the other node we will move this node inside or after
     */
    movePlanningNode(otherNodeId) {
        
        /*
         * check that this node is not the same as the other node.
         * if they are the same we don't need to do anything.
         */
        if (this.nodeId != otherNodeId) {
            if (this.ProjectService.isGroupNode(otherNodeId)) {
                // insert this node inside the group node
                this.ProjectService.movePlanningNodeInstanceInside(this.nodeId, otherNodeId);
            } else {
                // insert this node after the other node
                this.ProjectService.movePlanningNodeInstanceAfter(this.nodeId, otherNodeId);
            }
        }
        
        // perform any necessary updating
        this.planningNodeChanged();
    }
    
    planningNodeChanged() {
        this.$rootScope.$broadcast('planningNodeChanged');
    }
}

NavItemController.$inject = [
    '$rootScope',
    '$scope',
    '$element',
    'ProjectService',
    'StudentDataService'
];

export default NavItemController;
