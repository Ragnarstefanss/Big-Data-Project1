import matplotlib.pyplot as plt
import numpy as np

class ResultFile:
    def __init__(self, fname):
        self.i = 0
        self.lines = ResultFile.read_lines(fname)

    def get_next(self, map_to_int = False):
        line = self.lines[self.i]
        self.i += 1
        return map(int, line.rstrip().split()) if map_to_int else line.rstrip()

    def dump(self):
        self.i += 1

    def __bool__(self):
        return self.i < len(self.lines)

    
    @staticmethod
    def read_lines(fname):
        with open(fname) as f:
            return f.readlines()

class Data:
    def __init__(self, rf):
        rf.dump()
        self.iteration = int(rf.get_next().split()[1])
        rf.dump()
        self.S, self.E, self.N, self.W, self.I, self.M, self.B = rf.get_next(True)
        rf.dump()
        self.node_ids = list(rf.get_next(True))
        rf.dump()
        self.node_e_dist = list(rf.get_next(True))
        rf.dump()
        self.node_w_dist = list(rf.get_next(True))
        rf.dump()
        self.extent_ids = list(rf.get_next(True))
        rf.dump()
        self.node_to_extents = {}
        for _ in range(self.S):
            line = rf.get_next().split()
            self.node_to_extents[int(line[0][:-1])] = list(map(int, line[1:]))
        rf.dump()
        self.jumps = list(rf.get_next(True))  

    def process(self):
        _, axes = plt.subplots(nrows=1, ncols=2)
        axes[0].bar(list(range(self.S)), self.node_e_dist)
        axes[1].bar(list(range(self.S)), self.node_w_dist)
        plt.savefig(f"distribution{self.iteration}.png")
        plt.clf()
        plt.hist(self.jumps, len(set(self.jumps)))
        plt.savefig(f"jumps{self.iteration}.png")

    def validate(self):        
        for extent in self.node_to_extents[self.node_ids[0]]:
            assert extent <= self.node_ids[0] or self.node_ids[-1] < extent
        for i, n_id in enumerate(self.node_ids[1:]):
            for extent in self.node_to_extents[n_id]:
                assert self.node_ids[i] < extent <= n_id
        print("All extents belong to the correct node")
        # Can maybe add a test for fingertable here...
        print("Validation complete")
        
        
def process_results(rf, validate=True):
    while rf:
        data = Data(rf)
        if validate:
            data.validate()
        data.process()

def main():
    rf = ResultFile("results.txt")
    process_results(rf)


if __name__ == "__main__":
    main()