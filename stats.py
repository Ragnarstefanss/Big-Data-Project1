import matplotlib.pyplot as plt
import numpy as np
import seaborn as sns
sns.set(rc={'figure.figsize': (12, 10)})

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
        rf.dump()
        self.node_to_fingertable = {}
        for _ in range(self.S):
            line = rf.get_next().split()
            self.node_to_fingertable[int(line[0][:-1])] = list(map(int, line[1:]))


    def process(self):
        width = 0.50
    
        fig, ax = plt.subplots(nrows=1, ncols=2)
        fig.suptitle('Node E dist vs Node W dist -'+ str(self.iteration))
        barlist = ax[0].bar(list(range(self.S)), self.node_e_dist, width, label="Node e dist")
        barlist2 = ax[1].bar(list(range(self.S)), self.node_w_dist, width, label="Node w dist", color="blue")
        
        ax[0].set_ylabel('Dist')
        ax[0].set_xticklabels(list(range(self.S)))
        ax[1].set_xticklabels(list(range(self.S)))

        def color_barlist(bar, color_1, color_2):
            for i in range(len(list(range(self.S)))):
                if i % 2 == 0:
                    bar[i].set_color(color_1)
                else:
                    bar[i].set_color(color_2)

        color_barlist(barlist, "teal", "turquoise")
        color_barlist(barlist2, "orange", "tomato")

        '''
        def autolabel(rects):
            """
            Attach a text label above each bar displaying its height
            """
            for rect in rects:
                height = self.node_e_dist
                ax.text(rect.get_x() + rect.get_width()/2., 1.05*height,
                        '%d' % int(height),
                        ha='center', va='bottom')
        autolabel(barlist)
        '''
        ax[0].legend()
        ax[1].legend()
        plt.savefig(f"distribution{self.iteration}.png")


        '''
        '''
        plt.clf()
        plt.title("Jumps " + str(self.iteration), fontdict={'fontsize': 24})
        jumps = plt.hist(self.jumps, len(set(self.jumps)), color="teal")
        plt.savefig(f"jumps{self.iteration}.png")



    def validate(self):
        for extent in self.node_to_extents[self.node_ids[0]]:
            assert extent <= self.node_ids[0] or self.node_ids[-1] < extent
        for i, n_id in enumerate(self.node_ids[1:]):
            for extent in self.node_to_extents[n_id]:
                assert self.node_ids[i] < extent <= n_id
        print("All extents belong to the correct node")
        # Can maybe add a test for fingertable here...

        assert sorted(self.node_ids) == self.node_ids
        assert len(self.node_ids) == len(self.node_to_extents) == self.S
        assert all(0 <= n_id < 2**self.B for n_id in self.node_ids)

        for n_id in self.node_ids:
            for i, f_id in enumerate(self.node_to_fingertable[n_id]):
                z = (n_id + 2**i) % (2 ** self.B)
                if z > self.node_ids[-1] or z <= self.node_ids[0]:
                    assert f_id == self.node_ids[0]
                else:
                    place = -1
                    for j, x in enumerate(self.node_ids):
                        if x == f_id:
                            place = j
                            break
                    assert place != -1
                    if not (self.node_ids[place-1] < z <= self.node_ids[place]):
                        print(f"{n_id} has invalid fingertable offset 2**{i}")
                        print(f"Points at {z} and is mapped to {f_id}\nAll nodes are")
                        print(' '.join(map(str, self.node_ids)), flush=True)
                        assert False
        print("Fingertables valid")
        print("Validation complete")
        
        
def process_results(rf, validate=True, plot=True):
    while rf:
        data = Data(rf)
        if validate:
            data.validate()
        if plot:
            data.process()

def main():
    rf = ResultFile("results.txt")
    process_results(rf, validate=True, plot=True)


if __name__ == "__main__":
    main()
