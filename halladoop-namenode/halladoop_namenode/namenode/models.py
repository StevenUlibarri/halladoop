
class Registration(object):

    def __init__(self, nodeID, nodeIP, totalDiskSpaceMb, availableDiskSpaceMB, **kwargs):
        self.nodeID = nodeID
        self.nodeIP = nodeIP,
        self.totalDiskSpaceMb = totalDiskSpaceMb,
        self.availableDiskSpaceMB = availableDiskSpaceMB
